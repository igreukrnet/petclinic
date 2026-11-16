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
        Locator firstNameInput = page.locator("#owner-first-name, #firstName, input[name='firstName']").first();
        firstNameInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        if (request.firstName() != null) {
            firstNameInput.click();
            firstNameInput.fill(request.firstName());
        }
        if (request.lastName() != null) {
            Locator lastNameInput = page.locator("#owner-last-name, #lastName, input[name='lastName']").first();
            lastNameInput.click();
            lastNameInput.fill(request.lastName());
        }
        if (request.address() != null) {
            Locator addressInput = page.locator("#owner-address, #address, input[name='address']").first();
            addressInput.click();
            addressInput.fill(request.address());
        }
        if (request.city() != null) {
            Locator cityInput = page.locator("#owner-city, #city, input[name='city']").first();
            cityInput.click();
            cityInput.fill(request.city());
        }
        String digitsOnly = request.telephone() != null ? request.telephone().replaceAll("[^0-9]", "") : "";
        if (!digitsOnly.isEmpty()) {
            Locator telInput = page.locator("#owner-telephone, #telephone, input[name='telephone']").first();
            telInput.click();
            telInput.fill(digitsOnly);
        }
        return this;
    }

    @Step("Fill and update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        Locator addressInput = page.locator("#owner-address, #address, input[name='address']").first();
        addressInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        addressInput.click();
        addressInput.fill(newAddress);
        return this;
    }

    @Step("Submit owner form")
    public void submit() {
        waitForLoadingSpinner();

        Locator submitBtn = page.locator("form button[type='submit'], form button.btn-primary").first();
        submitBtn.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        submitBtn.click();

        // Crucial: Wait for Vue router to complete async submission & navigation:
        // On new owner create -> navigates to /owners (heading: <h2>Owners</h2>)
        // On owner edit -> navigates to /owners/details/:id (heading: <h2>Owner Information</h2>)
        try {
            page.locator("h2:has-text('Owners'), h2:has-text('Owner Information')")
                .first()
                .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        } catch (Exception e) {
            Locator alert = page.locator(".alert-danger");
            if (alert.count() > 0 && alert.first().isVisible()) {
                throw new AssertionError("Owner form submit failed with error: " + alert.first().textContent().trim());
            }
            throw e;
        }

        waitForLoadingSpinner();
    }
}
