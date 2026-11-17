package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * Common operations of the form pages (owner, pet, visit).
 * <p>
 * Validation is only observed here, never changed: the page reports what the constraint validation of
 * the browser says about a field, and which requests the page sent. Nothing makes the form look valid.
 */
public abstract class FormPage extends BasePage {

    private final List<String> writeRequests = Collections.synchronizedList(new ArrayList<>());

    protected FormPage(Page page) {
        super(page);
        // Every API request of the page passes through this handler before it leaves the browser.
        // fallback() hands it on unchanged (to other handlers, then to the network).
        page.route("**/api/**", route -> {
            if (!"GET".equals(route.request().method())) {
                writeRequests.add(route.request().method() + " " + URI.create(route.request().url()).getPath());
            }
            route.fallback();
        });
    }

    /** Sets the value of a field the way {@code fill} does: through the browser editing, so {@code maxlength} applies. */
    @Step("Fill {selector} with: {value}")
    public void fillField(String selector, String value) {
        page.locator(selector).fill(value);
    }

    /** Types into a field key by key, as a user does. */
    @Step("Type into {selector}: {text}")
    public void typeInto(String selector, String text) {
        page.locator(selector).pressSequentially(text);
    }

    public String valueOf(String selector) {
        return page.locator(selector).inputValue();
    }

    /**
     * Clicks the submit button of a form that is expected to be refused, and waits for the refusal to
     * show: either the browser marks the form invalid (nothing is sent), or the application shows its
     * error box (the server refused the request). Fails if neither happens.
     * <p>
     * The form checks its validity synchronously inside the click, so when this method returns after a
     * browser refusal, {@link #writeRequestsSent()} is complete.
     */
    @Step("Click submit and wait for the form to be refused")
    public void submitExpectingRefusal() {
        page.locator(MAIN_FORM_SUBMIT).click();
        assertThat(page.locator(MAIN_FORM + ":invalid, .alert-danger").first()).isVisible();
    }

    /** POST, PUT and DELETE requests to the API that this page has sent, like {@code POST /api/customer/owners}. */
    public List<String> writeRequestsSent() {
        return List.copyOf(writeRequests);
    }

    /**
     * What the constraint validation of the browser reports for the field: the name of the first failed
     * rule ({@code valueMissing}, {@code patternMismatch}, {@code tooLong}, {@code badInput}, ...), or an
     * empty string when the field is valid. Read only.
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
}
