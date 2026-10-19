package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import com.kjune922.waterapi.question.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class ChatExportTest {
    @Test void exportsAnswerAndSnapshotAndEscapesUnsafeContent() {
        Facility f = new Facility("펌프", FacilityType.PUMP, "내부");
        QuestionForm form = new QuestionForm(); form.setQuestion("=1+1"); form.setKeyword("");
        InspectionReport report = new InspectionReport(f, "누수, 패킹 \"교체\"\n<script>alert(1)</script>", LocalDate.now());
        QuestionHistory h = new QuestionHistory(f, form, "<script>alert(2)</script>", 1, List.of(new RecordEvidence(report)));
        h.recordModel("test"); ChatExportService e = new ChatExportService();
        assertThat(e.text(h)).contains("누수", "교체", "test", "조건 일치 1건");
        assertThat(e.html(h)).contains("&lt;script&gt;").doesNotContain("<script>");
        assertThat(e.csv(h)).startsWith("\ufeff").contains("'=1+1", "\"\"교체\"\"");
    }
}
