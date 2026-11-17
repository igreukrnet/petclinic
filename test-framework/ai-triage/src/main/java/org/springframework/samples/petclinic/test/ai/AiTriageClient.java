package org.springframework.samples.petclinic.test.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aeonbits.owner.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends a failed test to an OpenAI-compatible chat endpoint and returns the analysis as Markdown.
 * It never throws: a problem with the AI service must not hide or replace the failure of the test,
 * so every error comes back as a text that starts with a warning sign.
 */
public class AiTriageClient {

    private static final Logger log = LoggerFactory.getLogger(AiTriageClient.class);

    private static final String SYSTEM_PROMPT = """
        You are an expert in test automation and in diagnosing failures of microservice applications \
        (Spring Cloud, REST Assured, Playwright, MySQL, JDBI 3).
        Analyse the failed test, name the most likely root cause (application defect, infrastructure failure, \
        UI selector or timeout problem, network timeout, inconsistent database state) and give concrete steps to fix it.
        Answer in Markdown with three sections:
        1. **Summary**: what failed, in one or two sentences.
        2. **Likely cause**: the analysis.
        3. **How to fix**: exact actions.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiTriageConfig config;

    public AiTriageClient() {
        this(ConfigFactory.create(AiTriageConfig.class));
    }

    /** For tests and for callers that bring their own settings. */
    public AiTriageClient(AiTriageConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /** The settings that are required but not set, as a readable list. Empty when the client can be used. */
    public List<String> missingSettings() {
        List<String> missing = new java.util.ArrayList<>();
        if (isBlank(config.aiBaseUrl())) {
            missing.add("ai.base.url");
        }
        if (isBlank(config.aiApiKey())) {
            missing.add("ai.api.key");
        }
        if (isBlank(config.aiModel())) {
            missing.add("ai.model");
        }
        return missing;
    }

    public String analyzeFailure(String testIdentifier, Throwable throwable, String extraContext) {
        List<String> missing = missingSettings();
        if (!missing.isEmpty()) {
            return "⚠️ AI Triage is not configured. Missing settings: " + String.join(", ", missing);
        }
        try {
            String baseUrl = config.aiBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URI uri = URI.create(baseUrl + "/chat/completions");

            StringBuilder userMessage = new StringBuilder();
            userMessage.append("Failed test: ").append(testIdentifier).append("\n\n");
            userMessage.append("Exception type: ").append(throwable.getClass().getName()).append("\n");
            userMessage.append("Message: ").append(throwable.getMessage()).append("\n\n");

            if (!isBlank(extraContext)) {
                userMessage.append("Context:\n").append(extraContext).append("\n\n");
            }

            userMessage.append("Stack trace (first 25 lines):\n```\n");
            StackTraceElement[] trace = throwable.getStackTrace();
            int maxLines = Math.min(trace.length, 25);
            for (int i = 0; i < maxLines; i++) {
                userMessage.append("  at ").append(trace[i]).append("\n");
            }
            userMessage.append("```\n");

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", config.aiModel());
            payload.put("temperature", 0.2);
            payload.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userMessage.toString())
            ));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.aiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();

            log.info("Sending AI triage request to {} for failed test: {}", uri, testIdentifier);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode choices = objectMapper.readTree(response.body()).path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "AI Triage Response: " + response.body();
            }
            log.warn("AI Triage service responded with status {}: {}", response.statusCode(), response.body());
            return "⚠️ AI Triage error (HTTP " + response.statusCode() + "): " + response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "⚠️ AI Triage was interrupted: " + e.getMessage();
        } catch (Exception e) {
            // Deliberate: see the class comment. The cause is logged in full and reported in the text.
            log.error("Failed to execute AI Triage analysis", e);
            return "⚠️ AI Triage could not be completed: " + e.getMessage();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
