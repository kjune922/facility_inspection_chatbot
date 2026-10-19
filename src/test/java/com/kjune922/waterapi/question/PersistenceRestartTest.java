package com.kjune922.waterapi.question;

import com.kjune922.waterapi.WaterApplication;
import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.InspectionReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.nio.file.Path;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRestartTest {
    @TempDir Path temp;

    private ConfigurableApplicationContext open() {
        return new SpringApplicationBuilder(WaterApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=jdbc:h2:file:" + temp.resolve("restart-db"),
                     "--spring.jpa.hibernate.ddl-auto=update", "--spring.h2.console.enabled=false");
    }

    @Test
    void reloadsQuestionAndEvidenceAfterContextRestart() {
        Long facilityId;
        Long questionId;
        try (ConfigurableApplicationContext first = open()) {
            Facility facility = first.getBean(FacilityService.class)
                    .registerFacility("재시작 검증", FacilityType.PUMP, "부산");
            facilityId = facility.getId();
            first.getBean(InspectionReportService.class).registerInspection(
                    facilityId, "재시작 후에도 보존할 누수 기록", LocalDate.of(2026, 10, 1));
            QuestionForm form = new QuestionForm();
            form.setQuestion("누수 기록 확인"); form.setKeyword("누수");
            questionId = first.getBean(QuestionService.class).ask(facilityId, form).getId();
        }
        try (ConfigurableApplicationContext second = open()) {
            // 서비스 트랜잭션 밖에서도 결과를 검증할 수 있게 repository+EntityManager 대신
            // TransactionTemplate의 명시적 콜백을 사용합니다.
            org.springframework.transaction.support.TransactionTemplate tx = new org.springframework.transaction.support.TransactionTemplate(
                    second.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            final Long fid = facilityId;
            final Long qid = questionId;
            tx.execute(new org.springframework.transaction.support.TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(org.springframework.transaction.TransactionStatus status) {
                    QuestionHistory saved = second.getBean(QuestionService.class).findQuestion(fid, qid);
                    assertThat(saved.getAnswer()).contains("재시작 후에도 보존할 누수 기록");
                    assertThat(saved.getEvidence()).hasSize(1);
                    assertThat(saved.getEvidence().get(0).getContent()).contains("누수 기록");
                }
            });
        }
    }
}
