package com.kjune922.waterapi.question;

import com.kjune922.waterapi.facility.Facility;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "question_history")
@lombok.Getter
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class QuestionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 100)
    private String answerModel;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facility_id", nullable = false)
    private Facility facility;
    @Column(nullable = false, length = 50)
    private String facilityName;
    @Column(nullable = false, length = 500)
    private String question;
    @Column(nullable = false, length = 100)
    private String keyword;
    private LocalDate fromDate;
    private LocalDate toDate;
    private long matchedCount;
    @Lob @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String answer;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @ElementCollection
    @CollectionTable(name = "question_evidence", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "evidence_order")
    private List<RecordEvidence> evidence = new ArrayList<>();

    public QuestionHistory(Facility facility, QuestionForm form, String answer,
                           long matchedCount, List<RecordEvidence> evidence) {
        this.facility = facility;
        this.facilityName = facility.getName();
        this.question = form.getQuestion().trim();
        this.keyword = form.getKeyword();
        this.fromDate = form.getFromDate();
        this.toDate = form.getToDate();
        this.answer = answer;
        this.matchedCount = matchedCount;
        this.createdAt = LocalDateTime.now();
        this.evidence.addAll(evidence);
    }

    public void recordModel(String model) { this.answerModel = model; }
}
