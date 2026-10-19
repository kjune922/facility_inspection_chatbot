package com.kjune922.waterapi.question;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class LocalRecordAnswerClient implements RecordAnswerClient {
    @Override
    public String answer(String question, String facilityName, long matchedCount, List<RecordEvidence> evidence) {
        if (evidence.isEmpty()) {
            return "선택한 시설·기간·검색어에 맞는 점검 기록이 없습니다. 질문에 대한 판단 근거를 확인할 수 없습니다.";
        }
        StringBuilder result = new StringBuilder();
        result.append("[기록 조회 결과 / AI 미연결]\n");
        result.append(facilityName).append("의 조건 일치 기록 ").append(matchedCount)
              .append("건 중 최근 ").append(evidence.size()).append("건입니다.\n");
        result.append("질문: ").append(question).append("\n\n");
        for (RecordEvidence record : evidence) {
            result.append("점검 #").append(record.getInspectionId()).append(" | ")
                  .append(record.getInspectionDate()).append(" | ")
                  .append(record.getProcessingStatus().getDescription()).append("\n")
                  .append(record.getContent()).append("\n\n");
        }
        result.append("현재 버전은 조회한 기록을 정리합니다. 질문의 의미 해석, 고장 원인 추론, 조치 추천은 수행하지 않습니다.");
        return result.toString();
    }
}
