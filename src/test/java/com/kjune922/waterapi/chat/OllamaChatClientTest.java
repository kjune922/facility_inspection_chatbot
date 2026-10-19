package com.kjune922.waterapi.chat;

import com.kjune922.waterapi.facility.*;
import com.kjune922.waterapi.inspection.*;
import com.kjune922.waterapi.question.*;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class OllamaChatClientTest {
    private HttpServer server;
    private String body;
    private String reply = "{\"message\":{\"content\":\"점검 #1의 누수를 확인하세요\"},\"done\":true}";
    @BeforeEach void start() throws IOException {
        server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/chat", new HttpHandler() {
            @Override public void handle(HttpExchange exchange) throws IOException {
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] result = reply.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, result.length);
                exchange.getResponseBody().write(result); exchange.close();
            }
        });
        server.start();
    }
    @AfterEach void stop() { server.stop(0); }
    private OllamaChatClient client() {
        return new OllamaChatClient("http://127.0.0.1:" + server.getAddress().getPort(), "qwen2.5:3b");
    }
    private ChatContext context() {
        Facility f = new Facility("1번 펌프", FacilityType.PUMP, "내부");
        InspectionReport r = new InspectionReport(f, "연결부 누수", LocalDate.now());
        return new ChatContext(1L, f.getName(), 1, List.of(new RecordEvidence(r)), List.of("이전 질문: 누수는?"));
    }
    @Test void sendsOneModelAndSourcesAndParsesAnswer() {
        assertThat(client().answer(context(), "정리해줘")).contains("누수");
        assertThat(body).contains("qwen2.5:3b", "연결부 누수", "정리해줘", "이전 질문");
        assertThat(body).containsPattern("\"stream\"\\s*:\\s*false");
    }
    @Test void emptySourcesDoNotInvokeModel() {
        ChatContext c = new ChatContext(1L, "펌프", 0, List.of(), List.of());
        assertThat(client().answer(c, "상태는?")).contains("확인할 수 없습니다");
        assertThat(body).isNull();
    }
    @Test void invalidResponseIsAnExplicitFailure() {
        reply = "{\"message\":{\"content\":\"\"}}";
        try { client().answer(context(), "정리해줘"); Assertions.fail(); }
        catch (AiUnavailableException expected) { assertThat(expected.getMessage()).contains("빈 답변"); }
    }
}
