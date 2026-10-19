package com.kjune922.waterapi.question;

import com.kjune922.waterapi.facility.FacilityService;
import com.kjune922.waterapi.inspection.InspectionReportRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/facilities/{facilityId}/questions")
@lombok.RequiredArgsConstructor
public class QuestionController {
    private final FacilityService facilityService;
    private final QuestionService questionService;
    private final InspectionReportRepository inspectionRepository;

    @GetMapping
    public String list(@PathVariable("facilityId") Long facilityId,
                       @RequestParam(value = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("questionForm", new QuestionForm());
        addList(model, facilityId, page);
        return "questions/list";
    }

    @PostMapping
    public String ask(@PathVariable("facilityId") Long facilityId,
                      @Valid @ModelAttribute("questionForm") QuestionForm form,
                      BindingResult errors, Model model) {
        if (!errors.hasFieldErrors("fromDate") && !errors.hasFieldErrors("toDate")
                && form.getFromDate() != null && form.getToDate() != null
                && form.getFromDate().isAfter(form.getToDate())) {
            errors.rejectValue("toDate", "date.range", "종료일은 시작일 이후로 입력하세요.");
        }
        if (errors.hasErrors()) {
            addList(model, facilityId, 0);
            return "questions/list";
        }
        QuestionHistory history = questionService.ask(facilityId, form);
        return "redirect:/facilities/" + facilityId + "/questions/" + history.getId();
    }

    @GetMapping("/{questionId}")
    public String detail(@PathVariable("facilityId") Long facilityId,
                         @PathVariable("questionId") Long questionId, Model model) {
        QuestionHistory history = questionService.findQuestion(facilityId, questionId);
        List<Long> availableIds = new ArrayList<>();
        for (RecordEvidence evidence : history.getEvidence()) {
            if (inspectionRepository.existsById(evidence.getInspectionId())) {
                availableIds.add(evidence.getInspectionId());
            }
        }
        model.addAttribute("history", history);
        model.addAttribute("availableIds", availableIds);
        return "questions/detail";
    }

    private void addList(Model model, Long facilityId, int page) {
        model.addAttribute("facility", facilityService.findFacility(facilityId));
        model.addAttribute("histories", questionService.findHistory(facilityId, page));
    }

}
