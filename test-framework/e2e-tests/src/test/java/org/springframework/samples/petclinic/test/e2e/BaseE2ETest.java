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
import org.springframework.samples.petclinic.test.core.db.ConnectionFactory;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;

import java.io.ByteArrayInputStream;

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
    protected OwnerDao ownerDao;
    protected PetDao petDao;
    protected VisitDao visitDao;
    protected VetDao vetDao;

    @BeforeAll
    void initSuite() {
        config = ConfigurationManager.getConfig();
        petClinicClient = new PetClinicClient(config.gatewayUrl());
        ownerDao = ConnectionFactory.getOwnerDao();
        petDao = ConnectionFactory.getPetDao();
        visitDao = ConnectionFactory.getVisitDao();
        vetDao = ConnectionFactory.getVetDao();

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
    void tearDown(TestInfo testInfo) {
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

    @AfterAll
    void tearDownSuite() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    protected void deleteOwnerCascade(int ownerId) {
        try {
            var pets = petDao.findByOwnerId(ownerId);
            for (var pet : pets) {
                visitDao.deleteByPetId(pet.id());
            }
            petDao.deleteByOwnerId(ownerId);
            ownerDao.deleteById(ownerId);
        } catch (Exception e) {
            log.warn("Failed to delete owner cascade for ID {}: {}", ownerId, e.getMessage());
        }
    }
}
