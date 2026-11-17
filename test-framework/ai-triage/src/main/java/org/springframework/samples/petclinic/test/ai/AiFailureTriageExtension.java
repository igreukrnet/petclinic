package org.springframework.samples.petclinic.test.ai;

import io.qameta.allure.Allure;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When a test fails and {@code ai.triage.enabled=true}, sends the failure to the AI endpoint and attaches
 * the analysis to the Allure report.
 * <p>
 * The call is synchronous and can take up to 90 seconds, so the number of calls per run is limited
 * ({@code ai.triage.max.calls}): a broken environment that fails every test must not add an hour to the run
 * or send hundreds of stack traces to an external service.
 */
public class AiFailureTriageExtension implements TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(AiFailureTriageExtension.class);

    /** One budget for the whole JVM: the extension is created once per test class. */
    private static final TriageBudget BUDGET =
        new TriageBudget(ConfigFactory.create(AiTriageConfig.class).aiTriageMaxCalls());

    private final AiTriageConfig config = ConfigFactory.create(AiTriageConfig.class);
    private final AiTriageClient aiTriageClient = new AiTriageClient(config);

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (!config.aiTriageEnabled()) {
            log.debug("AI Failure Triage is disabled (ai.triage.enabled=false)");
            return;
        }

        String testName = context.getRequiredTestClass().getSimpleName() + "#" + context.getDisplayName();
        if (!BUDGET.tryAcquire()) {
            log.info("AI Triage skipped for {}: the limit of {} calls per run is used up", testName, BUDGET.limit());
            return;
        }
        log.info("Test failed: {}. Triggering AI Triage...", testName);

        String extraContext = "Class: " + context.getRequiredTestClass().getName() +
                              "\nMethod: " + context.getRequiredTestMethod().getName() +
                              "\nTags: " + context.getTags();

        String triageReport = aiTriageClient.analyzeFailure(testName, cause, extraContext);

        log.info("AI Triage Analysis for {}:\n{}", testName, triageReport);
        Allure.addAttachment("AI Failure Triage Analysis", "text/markdown", triageReport, ".md");
    }
}
