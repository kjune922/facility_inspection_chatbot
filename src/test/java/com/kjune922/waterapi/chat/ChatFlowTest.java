package com.kjune922.waterapi.chat;

import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import com.kjune922.waterapi.question.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatFlowTest {
    @Autowired FacilityService facilities;
    @Autowired InspectionReportService inspections;
    @Autowired ChatService chat;
    @Autowired QuestionHistoryRepository histories;
    @Autowired MockMvc mvc;
    @MockitoBean OllamaChatClient ai;
    private QuestionForm form() {
        QuestionForm f = new QuestionForm(); f.setQuestion("이 시설 상태를 정리해줘"); return f;
    }
    @Test void usesOnlySelectedFacilityAndReleasesTransactionBeforeAi() throws Exception {
        Facility a = facilities.registerFacility("대상", FacilityType.PUMP, "내부");
        Facility b = facilities.registerFacility("별도", FacilityType.PUMP, "내부");
        inspections.registerInspection(a.getId(), "배관 누수 확인", LocalDate.now());
        inspections.registerInspection(b.getId(), "별도 시설 비공개 기록", LocalDate.now());
        when(ai.getModel()).thenReturn("test-local");
        when(ai.answer(any(ChatContext.class), anyString())).thenAnswer(new Answer<String>() {
            @Override public String answer(InvocationOnMock call) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                ChatContext c = call.getArgument(0);
                assertThat(c.getFacilityId()).isEqualTo(a.getId());
                assertThat(c.getEvidence()).hasSize(1);
                assertThat(c.getEvidence().get(0).getContent()).isEqualTo("배관 누수 확인");
                return "주의 의견: 점검 기록에 누수가 있습니다.";
            }
        });
        QuestionHistory h = chat.ask(a.getId(), form());
        assertThat(histories.findById(h.getId())).isPresent();
        mvc.perform(get("/chat").param("facilityId", a.getId().toString()))
           .andExpect(status().isOk()).andExpect(view().name("chat/index"));
        mvc.perform(get("/chat/" + h.getId() + "/download").param("facilityId", a.getId().toString()).param("format", "txt"))
           .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("배관 누수 확인")));
        mvc.perform(get("/chat/" + h.getId() + "/download").param("facilityId", b.getId().toString()).param("format", "txt"))
           .andExpect(status().isNotFound());
    }
    @Test void failedAiDoesNotSaveAnAnswerAndKeepsQuestion() throws Exception {
        Facility a = facilities.registerFacility("실패 테스트", FacilityType.PUMP, "내부");
        inspections.registerInspection(a.getId(), "진동 증가", LocalDate.now());
        long before = histories.count();
        when(ai.answer(any(ChatContext.class), anyString())).thenThrow(new AiUnavailableException("Ollama 연결 확인"));
        mvc.perform(post("/chat").param("facilityId", a.getId().toString()).param("question", "최근 상태 알려줘"))
            .andExpect(status().isOk()).andExpect(model().attributeExists("aiError"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("최근 상태 알려줘")));
        assertThat(histories.count()).isEqualTo(before);
    }
    @Test void rejectsInvalidDateWithoutCallingModel() throws Exception {
        Facility a = facilities.registerFacility("날짜", FacilityType.PUMP, "내부");
        mvc.perform(post("/chat").param("facilityId", a.getId().toString()).param("question", "요약")
            .param("fromDate", "2026-10-19").param("toDate", "2026-10-01"))
            .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("questionForm", "toDate"));
        verify(ai, never()).answer(any(ChatContext.class), anyString());
    }
}
