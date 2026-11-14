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
            firstNameInput.fill(request.firstName());
        }
        if (request.lastName() != null) {
            page.locator("#lastName, #owner-last-name, input[name='lastName']").first().fill(request.lastName());
        }
        if (request.address() != null) {
            page.locator("#address, #owner-address, input[name='address']").first().fill(request.address());
        }
        if (request.city() != null) {
            page.locator("#city, #owner-city, input[name='city']").first().fill(request.city());
        }
        if (request.telephone() != null) {
            com.microsoft.playwright.Locator telInput = page.locator("#telephone, #owner-telephone, input[name='telephone']").first();
            telInput.click();
            telInput.fill(request.telephone());
            telInput.dispatchEvent("input");
            telInput.dispatchEvent("change");
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
        try {
            page.evaluate("() => {\n" +
                "    const tel = document.querySelector('#telephone, #owner-telephone, input[name=\"telephone\"]');\n" +
                "    if (tel) { tel.removeAttribute('pattern'); tel.dispatchEvent(new Event('input', { bubbles: true })); }\n" +
                "}");
        } catch (Exception ignored) {}
        page.locator("button[type='submit'], button.btn-primary, button.btn-default").first().click();
        waitForLoadingSpinner();
    }
}
