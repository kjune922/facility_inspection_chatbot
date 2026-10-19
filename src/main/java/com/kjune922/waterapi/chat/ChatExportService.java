package com.kjune922.waterapi.chat;
import com.kjune922.waterapi.question.*;
import org.springframework.stereotype.Service;
@Service
public class ChatExportService {
    public String text(QuestionHistory h) {
        StringBuilder b = new StringBuilder();
        b.append(h.getFacilityName()).append(" · 점검 기록 답변\n생성: ").append(h.getCreatedAt())
         .append("\n모델: ").append(h.getAnswerModel()).append("\n질문: ").append(h.getQuestion())
         .append("\n기간: ").append(h.getFromDate()).append(" ~ ").append(h.getToDate())
         .append(" (null = 제한 없음)\n검색어: ").append(h.getKeyword())
         .append("\n조건 일치 ").append(h.getMatchedCount()).append("건 중 최신 ").append(h.getEvidence().size())
         .append("건 기준\n\n").append(h.getAnswer()).append("\n\n참고 기록 (답변 당시 복사본)\n");
        for (RecordEvidence e : h.getEvidence()) {
            b.append("#").append(e.getInspectionId()).append(" / ").append(e.getInspectionDate())
             .append(" / ").append(e.getProcessingStatus().getDescription()).append("\n").append(e.getContent()).append("\n\n");
        }
        return b.toString();
    }
    public String html(QuestionHistory h) {
        return "<!doctype html><html lang=\"ko\"><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<title>WaterAPI 점검 답변</title><style>body{max-width:860px;margin:40px auto;padding:24px;font:16px/1.8 sans-serif;color:#203044}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:inherit}</style>"
            + "<h1>시설 점검 기록 답변</h1><pre>" + escapeHtml(text(h)) + "</pre></html>";
    }
    public String csv(QuestionHistory h) {
        StringBuilder b = new StringBuilder("\ufeff종류,시설,질문,답변,점검ID,점검일,처리단계,점검내용,모델,생성시각,검색어,시작일,종료일,조건일치건수\r\n");
        b.append(row("답변", h.getFacilityName(), h.getQuestion(), h.getAnswer(), "", "", "", "", h.getAnswerModel(),
            h.getCreatedAt().toString(), h.getKeyword(), String.valueOf(h.getFromDate()), String.valueOf(h.getToDate()), String.valueOf(h.getMatchedCount())));
        for (RecordEvidence e : h.getEvidence()) {
            b.append(row("근거", h.getFacilityName(), "", "", String.valueOf(e.getInspectionId()),
                e.getInspectionDate().toString(), e.getProcessingStatus().getDescription(), e.getContent(), "", "", "", "", "", ""));
        }
        return b.toString();
    }
    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
    private String row(String... cells) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) { b.append(','); }
            String cell = cells[i];
            if (cell == null) { cell = ""; }
            String stripped = cell.stripLeading();
            if (!stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0) { cell = "'" + cell; }
            b.append('"').append(cell.replace("\"", "\"\"")).append('"');
        }
        return b.append("\r\n").toString();
    }
}
