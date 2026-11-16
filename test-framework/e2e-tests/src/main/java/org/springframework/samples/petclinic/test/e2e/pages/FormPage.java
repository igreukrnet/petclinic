package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Step;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Common operations of the form pages (owner, pet, visit).
 * <p>
 * Validation is only observed here, never changed: the page reports what the browser's own constraint
 * validation says about a field, and which requests the page sent. Nothing makes the form look valid.
 */
public abstract class FormPage extends BasePage {

    private final List<String> writeRequests = Collections.synchronizedList(new ArrayList<>());

    protected FormPage(Page page) {
        super(page);
        page.onRequest(request -> {
            if (request.url().contains("/api/") && !"GET".equals(request.method())) {
                writeRequests.add(request.method() + " " + URI.create(request.url()).getPath());
            }
        });
    }

    /** Sets the value of a field the way {@code fill} does (no key events, no {@code maxlength}). */
    @Step("Fill {selector} with: {value}")
    public void fillField(String selector, String value) {
        page.locator(selector).fill(value);
    }

    /** Types into a field key by key, as a user does. The browser applies {@code maxlength}. */
    @Step("Type into {selector}: {text}")
    public void typeInto(String selector, String text) {
        page.locator(selector).pressSequentially(text);
    }

    public String valueOf(String selector) {
        return page.locator(selector).inputValue();
    }

    /**
     * Clicks the submit button and waits until the page has been quiet on the network.
     * Use it when the form is expected to stay: afterwards {@link #writeRequestsSent()} is complete.
     */
    @Step("Click submit and wait for the page to settle")
    public void clickSubmit() {
        page.locator(MAIN_FORM_SUBMIT).click();
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /** POST, PUT and DELETE requests to the API that this page has sent, like {@code POST /api/customer/owners}. */
    public List<String> writeRequestsSent() {
        return List.copyOf(writeRequests);
    }

    /**
     * What the browser's constraint validation reports for the field: the name of the first failed rule
     * ({@code valueMissing}, {@code patternMismatch}, {@code tooLong}, {@code badInput}, ...), or an empty
     * string when the field is valid. Read only.
     */
    public String invalidReason(String selector) {
        Object reason = page.locator(selector).evaluate("""
            el => {
              for (const rule in el.validity) {
                if (rule !== 'valid' && el.validity[rule]) { return rule; }
              }
              return '';
            }""");
        return String.valueOf(reason);
    }

    /** The message the browser would show next to the field. Empty when the field is valid. */
    public String validationMessage(String selector) {
        return String.valueOf(page.locator(selector).evaluate("el => el.validationMessage"));
    }

    /** Waits for the red error box of the application and returns its text. Fails if it does not appear. */
    @Step("Read the error message shown by the application")
    public String errorAlertText() {
        Locator alert = page.locator(".alert-danger");
        assertThat(alert.first()).isVisible();
        return alert.first().innerText().trim();
    }

    public boolean isErrorAlertShown() {
        return page.locator(".alert-danger").count() > 0;
    }

    /** Path of the current address, like {@code /owners/new}. */
    public String currentPath() {
        return URI.create(page.url()).getPath();
    }
}
