package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.facility.FacilityService;
import com.kjune922.waterapi.question.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final FacilityService facilities;
    private final QuestionService questions;
    private final ChatService chat;
    private final ChatExportService exports;
    private final OllamaChatClient ai;
    @GetMapping
    public String index(@RequestParam(value="facilityId", required=false) Long facilityId,
                        @RequestParam(value="page", defaultValue="0") int page, Model model) {
        model.addAttribute("questionForm", new QuestionForm());
        populate(facilityId, page, model);
        return "chat/index";
    }
    @PostMapping
    public String ask(@RequestParam("facilityId") Long facilityId,
                      @Valid @ModelAttribute("questionForm") QuestionForm form,
                      BindingResult errors, Model model) {
        if (!errors.hasErrors() && form.getFromDate() != null && form.getToDate() != null
                && form.getFromDate().isAfter(form.getToDate())) {
            errors.rejectValue("toDate", "range", "종료일은 시작일 이후로 입력하세요.");
        }
        if (!errors.hasErrors()) {
            try {
                QuestionHistory history = chat.ask(facilityId, form);
                return "redirect:/chat?facilityId=" + facilityId + "#answer-" + history.getId();
            } catch (AiUnavailableException ex) { model.addAttribute("aiError", ex.getMessage()); }
        }
        populate(facilityId, 0, model);
        return "chat/index";
    }
    private void populate(Long facilityId, int page, Model model) {
        model.addAttribute("facilities", facilities.findFacilities());
        model.addAttribute("modelName", ai.getModel());
        if (facilityId != null) {
            model.addAttribute("facility", facilities.findFacility(facilityId));
            model.addAttribute("histories", questions.findHistory(facilityId, page));
        }
    }
    @GetMapping("/{questionId}/download")
    public ResponseEntity<byte[]> download(@PathVariable("questionId") Long questionId,
            @RequestParam("facilityId") Long facilityId, @RequestParam("format") String format) {
        QuestionHistory h = questions.findQuestion(facilityId, questionId);
        String content; String type;
        if ("txt".equals(format)) { content = exports.text(h); type = "text/plain;charset=UTF-8"; }
        else if ("csv".equals(format)) { content = exports.csv(h); type = "text/csv;charset=UTF-8"; }
        else if ("html".equals(format)) { content = exports.html(h); type = "text/html;charset=UTF-8"; }
        else { throw new IllegalArgumentException("TXT, CSV, HTML 형식만 지원합니다."); }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=waterapi-answer-" + questionId + "." + format)
            .header("X-Content-Type-Options", "nosniff").header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(content.getBytes(StandardCharsets.UTF_8));
    }
}
