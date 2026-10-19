package com.kjune922.waterapi.question;

import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import com.kjune922.waterapi.exception.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionFlowTest {
    @Autowired FacilityService facilities;
    @Autowired InspectionReportService inspections;
    @Autowired QuestionService questions;
    @Autowired QuestionHistoryRepository histories;
    @Autowired EntityManager em;
    @Autowired MockMvc mvc;

    private Facility facility(String name) {
        return facilities.registerFacility(name, FacilityType.PUMP, "부산 테스트 구역");
    }
    private QuestionForm form(String keyword) {
        QuestionForm form = new QuestionForm();
        form.setQuestion("관련 점검 기록을 확인하고 싶습니다.");
        form.setKeyword(keyword);
        return form;
    }

    @Test
    void filtersFacilityDateAndKeywordAndPersistsSources() {
        Facility a = facility("대상 시설");
        Facility b = facility("다른 시설");
        inspections.registerInspection(a.getId(), "배관 누수 확인", LocalDate.of(2026, 10, 1));
        inspections.registerInspection(a.getId(), "누수 조치 완료", LocalDate.of(2026, 10, 2));
        inspections.registerInspection(a.getId(), "정상 상태", LocalDate.of(2026, 10, 2));
        inspections.registerInspection(b.getId(), "다른 시설 누수", LocalDate.of(2026, 10, 2));
        QuestionForm form = form("누수");
        form.setFromDate(LocalDate.of(2026, 10, 2));
        form.setToDate(LocalDate.of(2026, 10, 2));
        Long id = questions.ask(a.getId(), form).getId();
        em.flush(); em.clear();
        QuestionHistory saved = questions.findQuestion(a.getId(), id);
        assertThat(saved.getMatchedCount()).isEqualTo(1);
        assertThat(saved.getEvidence()).hasSize(1);
        assertThat(saved.getEvidence().get(0).getContent()).isEqualTo("누수 조치 완료");
        assertThat(saved.getAnswer()).doesNotContain("다른 시설 누수");
        assertThat(saved.getFacilityName()).isEqualTo("대상 시설");
    }

    @Test
    void limitsToTenMostRecentWithDeterministicTieBreak() {
        Facility a = facility("10건 제한");
        for (int i = 1; i <= 12; i++) {
            inspections.registerInspection(a.getId(), "기록 " + i, LocalDate.of(2026, 10, 1));
        }
        QuestionHistory saved = questions.ask(a.getId(), form(""));
        assertThat(saved.getMatchedCount()).isEqualTo(12);
        assertThat(saved.getEvidence()).hasSize(10);
        assertThat(saved.getEvidence().get(0).getContent()).isEqualTo("기록 12");
        assertThat(saved.getEvidence().get(9).getContent()).isEqualTo("기록 3");
    }

    @Test
    void treatsLikeWildcardsAsLiteralText() {
        Facility a = facility("검색어 문자");
        inspections.registerInspection(a.getId(), "효율 50%_! 확인", LocalDate.of(2026, 10, 1));
        inspections.registerInspection(a.getId(), "효율 50abc 확인", LocalDate.of(2026, 10, 1));
        QuestionHistory saved = questions.ask(a.getId(), form("%_!"));
        assertThat(saved.getMatchedCount()).isEqualTo(1);
        assertThat(saved.getEvidence().get(0).getContent()).contains("50%_!");
    }

    @Test
    void retainsSnapshotAfterOriginalEditAndDeletion() {
        Facility a = facility("스냅샷");
        InspectionReport report = inspections.registerInspection(a.getId(), "조회 당시 누수", LocalDate.of(2026, 10, 1));
        Long qid = questions.ask(a.getId(), form("누수")).getId();
        inspections.updateInspection(report.getId(), "수정된 내용", LocalDate.of(2026, 10, 2));
        inspections.deleteInspection(report.getId());
        em.flush(); em.clear();
        QuestionHistory saved = questions.findQuestion(a.getId(), qid);
        assertThat(saved.getEvidence().get(0).getContent()).isEqualTo("조회 당시 누수");
        assertThat(saved.getAnswer()).contains("조회 당시 누수").doesNotContain("수정된 내용");
        try {
            facilities.deleteFacility(a.getId());
            fail("질문이 있는 시설 삭제는 차단해야 합니다.");
        } catch (FacilityInUseException expected) {
            assertThat(expected.getMessage()).contains("질문 이력");
        }
    }

    @Test
    void emptyRecordsAreExplicitAndQuestionCannotCrossFacility() {
        Facility a = facility("기록 없음");
        Facility b = facility("다른 시설");
        QuestionHistory saved = questions.ask(a.getId(), form("누수"));
        assertThat(saved.getEvidence()).isEmpty();
        assertThat(saved.getAnswer()).contains("기록이 없습니다");
        try {
            questions.findQuestion(b.getId(), saved.getId());
            fail("다른 시설 경로로 이력 조회는 불가해야 합니다.");
        } catch (ResourceNotFoundException expected) {
            assertThat(expected.getMessage()).contains("찾을 수 없습니다");
        }
    }

    @Test
    void rendersPagesValidatesInputAndEscapesStoredText() throws Exception {
        Facility a = facility("화면 검증");
        inspections.registerInspection(a.getId(), "<script>alert(1)</script> 누수", LocalDate.of(2026, 10, 1));
        String path = "/facilities/" + a.getId() + "/questions";
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(view().name("questions/list"));
        long count = histories.count();
        mvc.perform(post(path).param("question", " "))
           .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("questionForm", "question"));
        mvc.perform(post(path).param("question", "질문").param("fromDate", "2026-10-03").param("toDate", "2026-10-01"))
           .andExpect(model().attributeHasFieldErrors("questionForm", "toDate"));
        mvc.perform(post(path).param("question", "질문").param("fromDate", "not-a-date"))
           .andExpect(model().attributeHasFieldErrors("questionForm", "fromDate"));
        assertThat(histories.count()).isEqualTo(count);
        QuestionHistory history = questions.ask(a.getId(), form("누수"));
        mvc.perform(get(path + "/" + history.getId())).andExpect(status().isOk())
           .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
        mvc.perform(get(path + "/999999")).andExpect(status().isNotFound());
        mvc.perform(get(path).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(post(path).param("question", "누수 기록 확인").param("keyword", "누수"))
           .andExpect(status().is3xxRedirection());
    }
}
