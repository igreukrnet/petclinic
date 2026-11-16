package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;

public class OwnerFormPage extends BasePage {

    public OwnerFormPage(Page page) {
        super(page);
    }

    @Step("Open Register Owner page")
    public OwnerFormPage openNew() {
        navigate("/owners/new");
        return this;
    }

    @Step("Open Edit Owner page for ID: {ownerId}")
    public OwnerFormPage openEdit(int ownerId) {
        navigate("/owners/" + ownerId + "/edit");
        return this;
    }

    @Step("Fill owner form: {request}")
    public OwnerFormPage fillForm(OwnerRequest request) {
        waitForLoadingSpinner();
        fillIfNotNull("#owner-first-name", request.firstName());
        fillIfNotNull("#owner-last-name", request.lastName());
        fillIfNotNull("#owner-address", request.address());
        fillIfNotNull("#owner-city", request.city());
        if (request.telephone() != null) {
            fillIfNotNull("#owner-telephone", request.telephone().replaceAll("[^0-9]", ""));
        }
        return this;
    }

    @Step("Update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        fillIfNotNull("#owner-address", newAddress);
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
