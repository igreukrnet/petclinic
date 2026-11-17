package org.springframework.samples.petclinic.test.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The live path of the triage client against a local fake of an OpenAI-compatible endpoint.
 * No real AI service and no key are needed, so this runs in CI.
 */
@Epic("AI Diagnostics")
@Feature("Failure Triage Service")
class AiTriageClientLocalEndpointTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private HttpServer server;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private final AtomicReference<String> body = new AtomicReference<>();

    private int status = 200;
    private String responseBody = "";

    @BeforeEach
    void startFakeEndpoint() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopFakeEndpoint() {
        server.stop(0);
    }

    private AiTriageClient client(String baseUrl) {
        return new AiTriageClient(new AiTriageConfig() {
            public boolean aiTriageEnabled() { return true; }
            public String aiBaseUrl() { return baseUrl; }
            public String aiApiKey() { return "test-key"; }
            public String aiModel() { return "test-model"; }
            public int aiTriageMaxCalls() { return 5; }
        });
    }

    @Test
    @DisplayName("Without a key, a model or an address nothing is sent and the missing settings are named")
    void missingSettingsAreReported() {
        AiTriageClient unconfigured = new AiTriageClient(new AiTriageConfig() {
            public boolean aiTriageEnabled() { return true; }
            public String aiBaseUrl() { return localUrl(); }
            public String aiApiKey() { return null; }
            public String aiModel() { return " "; }
            public int aiTriageMaxCalls() { return 5; }
        });

        String analysis = unconfigured.analyzeFailure("SomeTest#fails", new RuntimeException("x"), null);

        assertThat(analysis).startsWith("⚠").contains("ai.api.key", "ai.model").doesNotContain("ai.base.url");
        assertThat(path.get()).as("request sent to the endpoint").isNull();
    }

    private String localUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/";
    }

    @Test
    @DisplayName("The request goes to /chat/completions with the key, the model and the failure details")
    void sendsFailureToChatCompletions() throws IOException {
        responseBody = "{\"choices\":[{\"message\":{\"content\":\"root cause: the gateway\"}}]}";

        String analysis = client(localUrl()).analyzeFailure(
            "SomeTest#fails", new IllegalStateException("visits-service is down"), "Tags: [e2e]");

        assertThat(path.get()).isEqualTo("/v1/chat/completions");
        assertThat(authorization.get()).isEqualTo("Bearer test-key");
        JsonNode request = mapper.readTree(body.get());
        assertThat(request.path("model").asText()).isEqualTo("test-model");
        String userMessage = request.path("messages").get(1).path("content").asText();
        assertThat(userMessage).contains("SomeTest#fails", "IllegalStateException", "visits-service is down", "Tags: [e2e]");
        assertThat(analysis).isEqualTo("root cause: the gateway");
    }

    @Test
    @DisplayName("An error answer of the endpoint is reported with its status, not thrown")
    void reportsHttpError() {
        status = 500;
        responseBody = "overloaded";

        String analysis = client(localUrl()).analyzeFailure("SomeTest#fails", new RuntimeException("x"), null);

        assertThat(analysis).startsWith("⚠️").contains("HTTP 500").contains("overloaded");
    }

    @Test
    @DisplayName("An unreachable endpoint is reported, not thrown, so a triage problem never hides the test failure")
    void reportsUnreachableEndpoint() {
        int closedPort = server.getAddress().getPort();
        server.stop(0);

        String analysis = client("http://127.0.0.1:" + closedPort + "/v1").analyzeFailure(
            "SomeTest#fails", new RuntimeException("x"), null);

        assertThat(analysis).startsWith("⚠️");
    }

    @Test
    @DisplayName("An answer without choices is returned as raw text")
    void answerWithoutChoicesIsReturnedRaw() {
        responseBody = "{\"unexpected\":true}";

        String analysis = client(localUrl()).analyzeFailure("SomeTest#fails", new RuntimeException("x"), null);

        assertThat(analysis).contains("unexpected");
    }
}
