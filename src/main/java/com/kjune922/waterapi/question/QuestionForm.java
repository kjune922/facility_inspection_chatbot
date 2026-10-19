package com.kjune922.waterapi.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@lombok.Getter
@lombok.Setter
public class QuestionForm {
    @NotBlank(message = "질문을 입력하세요.")
    @Size(max = 500, message = "질문은 500자 이내로 입력하세요.")
    private String question;
    @Size(max = 100, message = "검색어는 100자 이내로 입력하세요.")
    private String keyword;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

}
