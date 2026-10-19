package com.kjune922.waterapi.question;

import com.kjune922.waterapi.inspection.InspectionReport;
import com.kjune922.waterapi.inspection.ProcessingStatus;
import jakarta.persistence.*;
import java.time.LocalDate;

@Embeddable
@lombok.Getter
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class RecordEvidence {
    private Long inspectionId;
    private LocalDate inspectionDate;
    @Column(length = 2000, nullable = false)
    private String content;
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    public RecordEvidence(InspectionReport report) {
        this.inspectionId = report.getId();
        this.inspectionDate = report.getInspectionDate();
        this.content = report.getContent();
        this.processingStatus = report.getProcessingStatus();
    }

}
