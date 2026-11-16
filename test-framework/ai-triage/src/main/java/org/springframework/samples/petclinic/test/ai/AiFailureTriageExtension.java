package org.springframework.samples.petclinic.test.ai;

import io.qameta.allure.Allure;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiFailureTriageExtension implements TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(AiFailureTriageExtension.class);
    private final AiTriageConfig config = ConfigFactory.create(AiTriageConfig.class);
    private final AiTriageClient aiTriageClient = new AiTriageClient();

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!config.aiTriageEnabled()) {
            log.debug("AI Failure Triage is disabled (ai.triage.enabled=false)");
            return;
        }

        String testName = context.getRequiredTestClass().getSimpleName() + "#" + context.getDisplayName();
        log.info("Test failed: {}. Triggering AI Triage...", testName);

        String extraContext = "Class: " + context.getRequiredTestClass().getName() +
                              "\nMethod: " + context.getRequiredTestMethod().getName() +
                              "\nTags: " + context.getTags();

        String triageReport = aiTriageClient.analyzeFailure(testName, cause, extraContext);

        log.info("AI Triage Analysis for {}:\n{}", testName, triageReport);
        Allure.addAttachment("🤖 AI Failure Triage Analysis", "text/markdown", triageReport, ".md");
    }
}
