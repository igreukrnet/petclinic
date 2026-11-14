package org.springframework.samples.petclinic.test.core.ai;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("AI Diagnostics")
@Feature("Failure Triage Service")
public class AiFailureTriageTest {

    @Test
    @Story("Automatic Root Cause Analysis")
    @DisplayName("Verify AI Triage Client analyzes test failure with gemini-3.8-flash")
    @Description("Sends simulated failed test stacktrace and metadata to the AI endpoint and validates diagnosis")
    void shouldGenerateStructuredTriageAnalysisForSimulatedFailure() {
        Assumptions.assumeTrue(
            ConfigurationManager.getConfig().aiTriageEnabled(),
            "AI Failure Triage is disabled in config (ai.triage.enabled=false). Skipping live AI integration test."
        );

        AiTriageClient client = new AiTriageClient();

        Throwable simulatedError = new RuntimeException("ConnectionRefused: Failed to connect to visits-service:8082 while aggregating owner details");

        String analysis = client.analyzeFailure(
            "ApiGatewayControllerTest#shouldAggregateOwnerVisits",
            simulatedError,
            "Context: Spring Cloud Gateway StripPrefix=2 routing to visits-service via Eureka"
        );

        assertThat(analysis).isNotBlank();
        assertThat(analysis).doesNotStartWith("⚠️");

        Allure.addAttachment("🤖 AI Failure Triage Sample", "text/markdown", analysis, ".md");
    }
}
