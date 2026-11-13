package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;

import java.io.ByteArrayInputStream;

public abstract class BasePage {

    protected final Page page;
    protected final String baseUrl;

    public BasePage(Page page) {
        this.page = page;
        this.baseUrl = ConfigurationManager.getConfig().gatewayUrl();
    }

    public Page getPage() {
        return page;
    }

    public void waitForLoadingSpinner() {
        try {
            Locator spinner = page.locator("loading-spinner, .spinner-border");
            if (spinner.count() > 0) {
                spinner.first().waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.DETACHED)
                    .setTimeout(10000));
            }
        } catch (Exception ignored) {
            // Spinner might have already detached
        }
    }

    @Step("Take screenshot: {name}")
    public void takeScreenshot(String name) {
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
    }

    @Step("Navigate to: {path}")
    public void navigate(String path) {
        String fullUrl = baseUrl + (path.startsWith("/") ? path : "/" + path);
        page.navigate(fullUrl);
        waitForLoadingSpinner();
        hideOverlays();
    }

    public void hideOverlays() {
        try {
            page.evaluate("() => document.querySelectorAll('.chatbox, #chatbox').forEach(el => el.style.display = 'none')");
        } catch (Exception ignored) {
        }
    }
}
