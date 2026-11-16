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
        // Playwright's DETACHED wait resolves immediately if the spinner is already gone,
        // so no exception handling is needed. A spinner stuck for 10s fails the test on purpose.
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

    @Step("Navigate to: {path}")
    public void navigate(String path) {
        String targetPath = (path == null || path.isEmpty() || path.equals("/")) ? "/" : (path.startsWith("/") ? path : "/" + path);
        String currentUrl = page.url();

        // If the browser is not yet on the app or on about:blank or error page, load root SPA first
        if (currentUrl == null || !currentUrl.startsWith(baseUrl) || currentUrl.contains("error") || page.locator("#app").count() == 0) {
            page.navigate(baseUrl + "/");
            page.waitForSelector("#main-navbar, #app", new Page.WaitForSelectorOptions().setTimeout(25000));
        }

        if (targetPath.equals("/") || targetPath.equals("/welcome")) {
            Locator homeLink = page.locator("a:has-text('Home'), a:has-text('HOME'), a[title*='home']").first();
            if (homeLink.count() > 0) {
                homeLink.click();
            }
        } else if (targetPath.equals("/owners/new")) {
            Locator newOwnerLink = page.locator("a:has-text('Register owner'), a:has-text('REGISTER OWNER'), a[title*='register owner'], a[href*='owners/new']").first();
            if (newOwnerLink.count() > 0) {
                newOwnerLink.click();
            } else {
                navigateToSpaRoute(targetPath);
            }
        } else if (targetPath.equals("/owners")) {
            Locator ownersLink = page.locator("a:has-text('Find owners'), a:has-text('FIND OWNERS'), a[title*='find owners'], a[href*='owners']").first();
            if (ownersLink.count() > 0) {
                ownersLink.click();
            } else {
                navigateToSpaRoute(targetPath);
            }
        } else if (targetPath.equals("/vets")) {
            Locator vetsLink = page.locator("a:has-text('Veterinarians'), a:has-text('VETERINARIANS'), a[title*='veterinarians'], a[href*='vets']").first();
            if (vetsLink.count() > 0) {
                vetsLink.click();
            } else {
                navigateToSpaRoute(targetPath);
            }
        } else {
            navigateToSpaRoute(targetPath);
        }

        waitForLoadingSpinner();
    }

    private void navigateToSpaRoute(String targetPath) {
        try {
            page.evaluate("target => {\n" +
                "    const appEl = document.querySelector('#app');\n" +
                "    if (appEl && appEl.__vue_app__ && appEl.__vue_app__.config && appEl.__vue_app__.config.globalProperties.$router) {\n" +
                "        appEl.__vue_app__.config.globalProperties.$router.push(target);\n" +
                "    } else {\n" +
                "        window.location.hash = '!' + target;\n" +
                "    }\n" +
                "}", targetPath);
        } catch (Exception e) {
            page.navigate(baseUrl + targetPath);
        }
    }
}
