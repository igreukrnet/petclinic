package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;

import java.net.URI;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Common parts of the page objects.
 * <p>
 * Pages are reached the way a user reaches them: the application is loaded at its root address and
 * everything else is a click on a link. Deep links are not used, because the gateway answers them with
 * 404 (for example {@code GET /owners/details/1}). That is an application defect, listed in the README,
 * and the tests must not hide it by calling the router from a script.
 */
public abstract class BasePage {

    /**
     * The main form of a page and its submit button. The chat widget also has a form with a submit button,
     * so it is excluded explicitly instead of relying on DOM order.
     */
    protected static final String MAIN_FORM = "form:not(.chatbox-footer)";
    protected static final String MAIN_FORM_SUBMIT = MAIN_FORM + " button[type='submit']";

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

    /** Loads the application at its root address, unless it is already loaded in this page. */
    @Step("Open the application")
    protected void openApplication() {
        if (!page.url().startsWith(baseUrl) || page.locator("#app").count() == 0) {
            page.navigate(baseUrl + "/");
        }
        assertThat(page.locator("a.nav-link[title='home page']")).isVisible();
    }

    /**
     * Clicks a link of the navigation bar. {@code title} is the title attribute of the link:
     * "home page", "find owners", "register owner" or "veterinarians".
     */
    @Step("Click the navigation link: {title}")
    protected void clickNavigation(String title) {
        openApplication();
        page.locator("a.nav-link[title='" + title + "']").click();
        waitForLoadingSpinner();
    }

    /** Path of the current address, like {@code /owners/new}. */
    public String currentPath() {
        return URI.create(page.url()).getPath();
    }
}
