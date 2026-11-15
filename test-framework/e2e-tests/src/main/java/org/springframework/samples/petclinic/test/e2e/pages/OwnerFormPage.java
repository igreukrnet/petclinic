package org.springframework.samples.petclinic.test.e2e.pages;

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
        com.microsoft.playwright.Locator firstNameInput = page.locator("#firstName, #owner-first-name, input[name='firstName']").first();
        firstNameInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(15000));
        if (request.firstName() != null) {
            firstNameInput.click();
            firstNameInput.fill(request.firstName());
        }
        if (request.lastName() != null) {
            com.microsoft.playwright.Locator lastNameInput = page.locator("#lastName, #owner-last-name, input[name='lastName']").first();
            lastNameInput.click();
            lastNameInput.fill(request.lastName());
        }
        if (request.address() != null) {
            com.microsoft.playwright.Locator addressInput = page.locator("#address, #owner-address, input[name='address']").first();
            addressInput.click();
            addressInput.fill(request.address());
        }
        if (request.city() != null) {
            com.microsoft.playwright.Locator cityInput = page.locator("#city, #owner-city, input[name='city']").first();
            cityInput.click();
            cityInput.fill(request.city());
        }
        if (request.telephone() != null) {
            // Strip non-digits to satisfy pattern="[0-9]{1,12}" on the telephone input
            String digitsOnly = request.telephone().replaceAll("[^0-9]", "");
            com.microsoft.playwright.Locator telInput = page.locator("#telephone, #owner-telephone, input[name='telephone']").first();
            telInput.click();
            telInput.fill(digitsOnly);
        }
        return this;
    }

    @Step("Fill and update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        com.microsoft.playwright.Locator addressInput = page.locator("#address, #owner-address, input[name='address']").first();
        addressInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(15000));
        addressInput.click();
        addressInput.fill(newAddress);
        addressInput.dispatchEvent("input");
        addressInput.dispatchEvent("change");
        return this;
    }

    @Step("Submit owner form")
    public void submit() {
        // Click submit and wait for navigation away from the form (router.push to /owners or /owners/details/:id)
        page.locator("button[type='submit'], button.btn-primary, button.btn-default").first().click();
        // Wait until the URL changes (form navigates away on success) or up to 10s
        try {
            String currentUrl = page.url();
            page.waitForURL(url -> !url.equals(currentUrl), new Page.WaitForURLOptions().setTimeout(10000));
        } catch (Exception ignored) {
            // If URL didn't change, the submit may have been blocked — waitForLoadingSpinner handles UI
        }
        waitForLoadingSpinner();
    }
}
