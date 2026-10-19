package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.question.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatDataService data;
    private final OllamaChatClient ai;
    // No database transaction is held while waiting for local inference.
    public QuestionHistory ask(Long facilityId, QuestionForm form) {
        ChatContext context = data.prepare(facilityId, form);
        String answer = ai.answer(context, form.getQuestion());
        String model = ai.getModel();
        if (context.getEvidence().isEmpty()) { model = "기록 없음 · AI 미호출"; }
        return data.save(context, form, answer, model);
    }
}
