package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;

import java.io.ByteArrayInputStream;

public abstract class BasePage {

    /**
     * Submit button of the page's main form. The chat widget also has a form with a submit button,
     * so it is excluded explicitly instead of relying on DOM order.
     */
    protected static final String MAIN_FORM_SUBMIT = "form:not(.chatbox-footer) button[type='submit']";

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
        // DETACHED resolves immediately when the spinner is already gone.
        // A spinner that stays for 10 s fails the test on purpose.
        Locator spinner = page.locator("loading-spinner, .spinner-border");
        if (spinner.count() > 0) {
            spinner.first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.DETACHED)
                .setTimeout(10000));
        }
    }

    @Step("Take screenshot: {name}")
    public void takeScreenshot(String name) {
        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
        Allure.addAttachment(name, "image/png", new ByteArrayInputStream(screenshot), ".png");
    }

    /**
     * Routes inside the SPA with the Vue router.
     * <p>
     * A full page load of a deep link does not work: the gateway answers
     * {@code GET /owners/details/1} with 404 JSON, so only the root document is served.
     * The router push below is the same code path as an in-app link click. It depends on Vue internals
     * ({@code __vue_app__} on {@code #app}); if that changes, this method fails loudly instead of falling back.
     */
    @Step("Navigate to: {path}")
    public void navigate(String path) {
        String targetPath = (path == null || path.isEmpty()) ? "/" : (path.startsWith("/") ? path : "/" + path);
        ensureAppLoaded();
        page.evaluate(
            "target => document.querySelector('#app').__vue_app__.config.globalProperties.$router.push(target)",
            targetPath
        );
        waitForLoadingSpinner();
    }

    private void ensureAppLoaded() {
        if (!page.url().startsWith(baseUrl) || page.locator("#app").count() == 0) {
            page.navigate(baseUrl + "/");
            page.waitForSelector("#app", new Page.WaitForSelectorOptions().setTimeout(25000));
        }
    }
}
