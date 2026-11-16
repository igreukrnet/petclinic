package org.springframework.samples.petclinic.test.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.qameta.allure.Allure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.test.core.ai.AiFailureTriageExtension;
import org.springframework.samples.petclinic.test.core.api.PetClinicClient;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.config.TestConfig;
import org.springframework.samples.petclinic.test.core.db.Database;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public abstract class BaseE2ETest {

    private static final Logger log = LoggerFactory.getLogger(BaseE2ETest.class);

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    protected TestConfig config;
    protected PetClinicClient petClinicClient;
    protected Database database;
    protected OwnerDao ownerDao;
    protected PetDao petDao;
    protected VisitDao visitDao;
    protected VetDao vetDao;

    /** Owners created by the current test. Deleted in {@link #deleteTrackedOwners()} even when the test fails. */
    private final List<Integer> createdOwnerIds = new ArrayList<>();

    @BeforeAll
    void initSuite() {
        config = ConfigurationManager.getConfig();
        petClinicClient = new PetClinicClient(config.gatewayUrl());
        database = Database.fromConfig();
        ownerDao = database.owners();
        petDao = database.pets();
        visitDao = database.visits();
        vetDao = database.vets();

        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
        log.info("Playwright Chromium browser launched (headless={})", headless);
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
        page = context.newPage();
        page.onConsoleMessage(msg -> log.info("BROWSER LOG [{}]: {}", msg.type(), msg.text()));
        page.onPageError(err -> log.error("BROWSER PAGE ERROR: {}", err));
    }

    @AfterEach
    void captureScreenshotAndClosePage(TestInfo testInfo) {
        try {
            if (page != null) {
                byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                Allure.addAttachment(
                    "📸 Screenshot: " + testInfo.getDisplayName(),
                    "image/png",
                    new ByteArrayInputStream(screenshot),
                    ".png"
                );
            }
        } catch (Exception e) {
            // Screenshot is diagnostics only; the test result must not depend on it.
            log.debug("Could not capture screenshot: {}", e.getMessage());
        } finally {
            if (page != null) {
                page.close();
            }
            if (context != null) {
                context.close();
            }
        }
    }

    /**
     * Runs even when the test failed. Cleanup failures are reported as failures of the test:
     * leftover data would break later tests, so they must not be swallowed.
     */
    @AfterEach
    void deleteTrackedOwners() {
        List<Integer> ids = List.copyOf(createdOwnerIds);
        createdOwnerIds.clear();

        List<RuntimeException> failures = new ArrayList<>();
        for (int ownerId : ids) {
            try {
                deleteOwnerCascade(ownerId);
            } catch (RuntimeException e) {
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException error = new IllegalStateException(
                "Cleanup failed for owner ids " + ids + "; database may contain leftover rows");
            failures.forEach(error::addSuppressed);
            throw error;
        }
    }

    @AfterAll
    void tearDownSuite() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (database != null) {
            database.close();
        }
    }

    /** Registers an owner for deletion at the end of the current test. Call it right after the owner is created. */
    protected void trackOwner(int ownerId) {
        createdOwnerIds.add(ownerId);
    }

    /** Deletes an owner with its pets and visits, in FK order. Throws on failure. */
    protected void deleteOwnerCascade(int ownerId) {
        for (var pet : petDao.findByOwnerId(ownerId)) {
            visitDao.deleteByPetId(pet.id());
        }
        petDao.deleteByOwnerId(ownerId);
        ownerDao.deleteById(ownerId);
    }
}
