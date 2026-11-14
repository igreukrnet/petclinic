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
        page.locator("#owner-first-name").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(15000));
        if (request.firstName() != null) {
            page.locator("#owner-first-name").fill(request.firstName());
        }
        if (request.lastName() != null) {
            page.locator("#owner-last-name").fill(request.lastName());
        }
        if (request.address() != null) {
            page.locator("#owner-address").fill(request.address());
        }
        if (request.city() != null) {
            page.locator("#owner-city").fill(request.city());
        }
        if (request.telephone() != null) {
            page.locator("#owner-telephone").fill(request.telephone());
        }
        return this;
    }

    @Step("Fill and update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        page.locator("#owner-address").fill(newAddress);
        return this;
    }

    @Step("Submit owner form")
    public void submit() {
        page.locator("button.btn-primary[type='submit']").click();
        waitForLoadingSpinner();
    }
}
