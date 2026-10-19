package com.kjune922.waterapi.question;

import com.kjune922.waterapi.exception.ResourceNotFoundException;
import com.kjune922.waterapi.facility.Facility;
import com.kjune922.waterapi.facility.FacilityService;
import com.kjune922.waterapi.inspection.InspectionReport;
import com.kjune922.waterapi.inspection.InspectionReportRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@lombok.RequiredArgsConstructor
public class QuestionService {
    private final FacilityService facilityService;
    private final InspectionReportRepository inspectionRepository;
    private final QuestionHistoryRepository historyRepository;
    private final RecordAnswerClient answerClient;

    @Transactional
    public QuestionHistory ask(Long facilityId, QuestionForm form) {
        Facility facility = facilityService.findFacility(facilityId);
        validate(form);
        String keyword = "";
        if (form.getKeyword() != null) {
            keyword = form.getKeyword().trim();
        }
        form.setKeyword(keyword);
        LocalDate from = form.getFromDate();
        LocalDate to = form.getToDate();
        if (from == null) { from = LocalDate.of(1, 1, 1); }
        if (to == null) { to = LocalDate.of(9999, 12, 31); }
        // LIKE의 %, _, !를 일반 문자로 검색하도록 이스케이프합니다.
        String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
        Page<InspectionReport> page = inspectionRepository.searchRecords(
                facilityId, from, to, pattern, PageRequest.of(0, 10));
        List<RecordEvidence> evidence = new ArrayList<>();
        for (InspectionReport report : page.getContent()) {
            evidence.add(new RecordEvidence(report));
        }
        String answer = answerClient.answer(form.getQuestion().trim(), facility.getName(),
                page.getTotalElements(), evidence);
        QuestionHistory history = new QuestionHistory(facility, form, answer,
                page.getTotalElements(), evidence);
        return historyRepository.save(history);
    }

    private void validate(QuestionForm form) {
        if (form == null || form.getQuestion() == null || form.getQuestion().isBlank()
                || form.getQuestion().length() > 500) {
            throw new IllegalArgumentException("질문은 1~500자로 입력하세요.");
        }
        if (form.getKeyword() != null && form.getKeyword().length() > 100) {
            throw new IllegalArgumentException("검색어는 100자 이내로 입력하세요.");
        }
        if (form.getFromDate() != null && form.getToDate() != null
                && form.getFromDate().isAfter(form.getToDate())) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
    }

    public Page<QuestionHistory> findHistory(Long facilityId, int page) {
        facilityService.findFacility(facilityId);
        if (page < 0) { throw new IllegalArgumentException("페이지는 0 이상이어야 합니다."); }
        return historyRepository.findByFacility_IdOrderByCreatedAtDescIdDesc(facilityId, PageRequest.of(page, 10));
    }

    public QuestionHistory findQuestion(Long facilityId, Long questionId) {
        QuestionHistory history = historyRepository.findByIdAndFacility_Id(questionId, facilityId).orElse(null);
        if (history == null) { throw new ResourceNotFoundException("질문 기록을 찾을 수 없습니다."); }
        return history;
    }

}
