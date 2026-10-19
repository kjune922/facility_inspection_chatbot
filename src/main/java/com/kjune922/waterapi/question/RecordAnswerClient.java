package com.kjune922.waterapi.question;

import java.util.List;

// 나중에 Ollama 구현체를 연결할 경계입니다. 현재는 네트워크 호출이 없습니다.
public interface RecordAnswerClient {
    String answer(String question, String facilityName, long matchedCount, List<RecordEvidence> evidence);
}
