package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OwnerFormPage extends FormPage {

    public static final String FIRST_NAME = "#owner-first-name";
    public static final String LAST_NAME = "#owner-last-name";
    public static final String ADDRESS = "#owner-address";
    public static final String CITY = "#owner-city";
    public static final String TELEPHONE = "#owner-telephone";

    public OwnerFormPage(Page page) {
        super(page);
    }

    @Step("Open Register Owner page")
    public OwnerFormPage openNew() {
        clickNavigation("register owner");
        assertThat(page.locator(FIRST_NAME)).isVisible();
        return this;
    }

    @Step("Open Edit Owner page for ID: {ownerId}")
    public OwnerFormPage openEdit(int ownerId) {
        new OwnerDetailsPage(page).open(ownerId);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit Owner")).click();
        waitForLoadingSpinner();
        assertThat(page.locator(FIRST_NAME)).isVisible();
        return this;
    }

    @Step("Fill owner form: {request}")
    public OwnerFormPage fillForm(OwnerRequest request) {
        waitForLoadingSpinner();
        fillIfNotNull(FIRST_NAME, request.firstName());
        fillIfNotNull(LAST_NAME, request.lastName());
        fillIfNotNull(ADDRESS, request.address());
        fillIfNotNull(CITY, request.city());
        fillIfNotNull(TELEPHONE, request.telephone());
        return this;
    }

    @Step("Update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        fillIfNotNull(ADDRESS, newAddress);
        return this;
    }

    @Step("Submit owner form")
    public void submit() {
        waitForLoadingSpinner();
        page.locator(MAIN_FORM_SUBMIT).click();

        // Vue router navigates after the async submit:
        // new owner -> /owners (<h2>Owners</h2>), edit -> /owners/details/:id (<h2>Owner Information</h2>).
        Locator success = page.locator("h2:text-is('Owners'), h2:text-is('Owner Information')");
        Locator alert = page.locator(".alert-danger");
        try {
            success.first().waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (RuntimeException e) {
            if (alert.count() > 0 && alert.first().isVisible()) {
                throw new AssertionError("Owner form submit failed with error: " + alert.first().textContent().trim(), e);
            }
            throw e;
        }
        waitForLoadingSpinner();
    }

    private void fillIfNotNull(String selector, String value) {
        if (value != null) {
            page.locator(selector).fill(value);
        }
    }
}
