package org.springframework.samples.petclinic.test.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.config.TestConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AiTriageClient {

    private static final Logger log = LoggerFactory.getLogger(AiTriageClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TestConfig config;

    public AiTriageClient() {
        this.config = ConfigurationManager.getConfig();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public String analyzeFailure(String testIdentifier, Throwable throwable, String extraContext) {
        try {
            String baseUrl = config.aiBaseUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URI uri = URI.create(baseUrl + "/chat/completions");

            String systemPrompt = """
                Ты эксперт по автоматизации тестирования и диагностике сбоев микросервисных приложений (Spring Cloud, REST Assured, Playwright, MySQL, JDBI 3).
                Твоя задача — проанализировать упавший тест, определить вероятную первопричину (дефект бизнес-логики, инфраструктурный сбой, ошибка селектора/таймаута UI, сетевой тайм-аут, неконсистентность БД) и дать конкретные шаги исправления.
                Ответ оформи структурированно в Markdown на русском языке:
                1. 🔍 **Суть сбоя**: краткое резюме.
                2. 🎯 **Вероятная причина**: детализированный анализ.
                3. 🛠️ **Рекомендации по устранению**: точные действия для починки.
                """;

            StringBuilder userMessage = new StringBuilder();
            userMessage.append("Упавший тест: ").append(testIdentifier).append("\n\n");
            userMessage.append("Тип исключения: ").append(throwable.getClass().getName()).append("\n");
            userMessage.append("Сообщение: ").append(throwable.getMessage()).append("\n\n");

            if (extraContext != null && !extraContext.isBlank()) {
                userMessage.append("Дополнительный контекст:\n").append(extraContext).append("\n\n");
            }

            userMessage.append("Стектрейс (первые 25 строк):\n```\n");
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
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userMessage.toString())
            ));

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + config.aiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            log.info("Sending AI triage request to {} for failed test: {}", uri, testIdentifier);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    return choices.get(0).path("message").path("content").asText();
                }
                return "AI Triage Response: " + response.body();
            } else {
                log.warn("AI Triage service responded with status {}: {}", response.statusCode(), response.body());
                return "⚠️ AI Triage error (HTTP " + response.statusCode() + "): " + response.body();
            }
        } catch (Exception e) {
            log.error("Failed to execute AI Triage analysis", e);
            return "⚠️ Не удалось получить анализ AI Triage: " + e.getMessage();
        }
    }
}
