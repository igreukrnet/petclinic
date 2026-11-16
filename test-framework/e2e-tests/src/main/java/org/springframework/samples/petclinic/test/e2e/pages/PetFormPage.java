package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;

public class PetFormPage extends BasePage {

    public PetFormPage(Page page) {
        super(page);
    }

    @Step("Open New Pet form for owner ID: {ownerId}")
    public PetFormPage openNew(int ownerId) {
        navigate("/owners/" + ownerId + "/new-pet");
        return this;
    }

    @Step("Fill pet form: {request}")
    public PetFormPage fillForm(PetRequest request) {
        waitForLoadingSpinner();
        Locator nameInput = page.locator("#name, #pet-name, input[name='name']").first();
        nameInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        if (request.name() != null) {
            nameInput.fill(request.name());
        }
        if (request.birthDate() != null) {
            page.locator("#birthDate, #pet-birth-date, input[name='birthDate']").first().fill(request.birthDate());
        }
        if (request.typeId() > 0) {
            page.locator("#type, #pet-type, select[name='type']").first()
                .selectOption(String.valueOf(request.typeId()));
        }
        return this;
    }

    @Step("Submit pet form")
    public void submit() {
        page.locator("form button[type='submit'], button.btn-primary, button.btn-default").first().click();
        page.locator("h2:has-text('Owner Information')").first()
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        waitForLoadingSpinner();
    }
}
