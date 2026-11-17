package org.springframework.samples.petclinic.test.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base of the tests that drive the browser. One browser per test class, one context and page per test.
 * <p>
 * Diagnostics are attached to the Allure report only when the test fails: a full-page screenshot,
 * the Playwright trace (open it with {@code npx playwright show-trace trace.zip} or at trace.playwright.dev)
 * and the errors the browser console reported. Passed tests leave no attachments.
 */
public abstract class BaseUiE2ETest extends BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(BaseUiE2ETest.class);

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    protected Page page;

    private final List<String> browserErrors = Collections.synchronizedList(new ArrayList<>());
    private boolean tracing;

    /**
     * Runs right after the test method and before any {@code @AfterEach}, while the page is still open.
     * This is the only place that knows whether the test failed.
     */
    @RegisterExtension
    final AfterTestExecutionCallback failureDiagnostics = extensionContext -> {
        if (extensionContext.getExecutionException().isPresent()) {
            attachFailureDiagnostics();
        }
    };

    @BeforeAll
    void launchBrowser() {
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        log.info("Playwright Chromium browser launched (headless={})", headless);
    }

    @BeforeEach
    void createContextAndPage() {
        browserErrors.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true));
        tracing = true;
        page = context.newPage();
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                browserErrors.add("console.error: " + msg.text());
            }
        });
        page.onPageError(error -> browserErrors.add("uncaught: " + error));
    }

    /**
     * Diagnostics must never change the result of the test, so a problem here is logged and not thrown.
     * Each attachment is independent: a failed screenshot does not lose the trace.
     */
    private void attachFailureDiagnostics() {
        try {
            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            Allure.addAttachment("Screenshot at failure", "image/png", new ByteArrayInputStream(screenshot), ".png");
        } catch (RuntimeException e) {
            log.warn("Could not capture the screenshot: {}", e.getMessage());
        }
        try {
            Path trace = Files.createTempFile("playwright-trace-", ".zip");
            context.tracing().stop(new Tracing.StopOptions().setPath(trace));
            tracing = false;
            Allure.addAttachment("Playwright trace", "application/zip", new ByteArrayInputStream(Files.readAllBytes(trace)), ".zip");
            Files.deleteIfExists(trace);
        } catch (RuntimeException | IOException e) {
            log.warn("Could not save the Playwright trace: {}", e.getMessage());
        }
        if (!browserErrors.isEmpty()) {
            Allure.addAttachment("Browser errors", "text/plain", String.join("\n", browserErrors));
        }
    }

    @AfterEach
    void closeContextAndPage() {
        if (context != null) {
            if (tracing) {
                // The test passed: the trace is discarded.
                context.tracing().stop();
                tracing = false;
            }
            context.close();
            context = null;
            page = null;
        }
    }

    @AfterAll
    void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
