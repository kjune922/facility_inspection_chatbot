package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import com.kjune922.waterapi.question.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.time.LocalDate;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatDataService {
    private final FacilityService facilities;
    private final InspectionReportRepository inspections;
    private final QuestionHistoryRepository histories;

    public ChatContext prepare(Long facilityId, QuestionForm form) {
        Facility facility = facilities.findFacility(facilityId);
        if (form.getQuestion() == null || form.getQuestion().isBlank() || form.getQuestion().length() > 500) {
            throw new IllegalArgumentException("질문은 1~500자로 입력하세요.");
        }
        if (form.getKeyword() == null) { form.setKeyword(""); }
        form.setKeyword(form.getKeyword().trim());
        if (form.getKeyword().length() > 100) { throw new IllegalArgumentException("검색어는 100자 이내로 입력하세요."); }
        LocalDate from = form.getFromDate();
        LocalDate to = form.getToDate();
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
        }
        if (from == null) { from = LocalDate.of(1, 1, 1); }
        if (to == null) { to = LocalDate.of(9999, 12, 31); }
        String pattern = "%" + form.getKeyword().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Page<InspectionReport> records = inspections.searchRecords(facilityId, from, to, pattern, PageRequest.of(0, 10));
        List<RecordEvidence> evidence = new ArrayList<>();
        int characters = 0;
        for (InspectionReport report : records.getContent()) {
            if (characters + report.getContent().length() > 6000) { break; }
            evidence.add(new RecordEvidence(report));
            characters += report.getContent().length();
        }
        List<String> previous = new ArrayList<>();
        List<QuestionHistory> recent = histories.findByFacility_IdOrderByCreatedAtDescIdDesc(facilityId, PageRequest.of(0, 4)).getContent();
        for (int i = recent.size() - 1; i >= 0; i--) {
            QuestionHistory h = recent.get(i);
            String answer = h.getAnswer();
            if (answer.length() > 500) { answer = answer.substring(0, 500); }
            previous.add("이전 질문: " + h.getQuestion() + "\n이전 답변(검증된 자료 아님): " + answer);
        }
        return new ChatContext(facilityId, facility.getName(), records.getTotalElements(), evidence, previous);
    }
    @Transactional
    public QuestionHistory save(ChatContext context, QuestionForm form, String answer, String model) {
        Facility facility = facilities.findFacility(context.getFacilityId());
        QuestionHistory h = new QuestionHistory(facility, form, answer, context.getMatchedCount(), context.getEvidence());
        h.recordModel(model);
        return histories.save(h);
    }
}
