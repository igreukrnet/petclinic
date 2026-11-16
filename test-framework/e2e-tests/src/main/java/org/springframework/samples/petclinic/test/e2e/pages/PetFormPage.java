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
        page.locator("#pet-name").fill(request.name());
        page.locator("#pet-birth-date").fill(request.birthDate());
        page.locator("#pet-type").selectOption(String.valueOf(request.typeId()));
        return this;
    }

    @Step("Submit pet form")
    public void submit() {
        page.locator(MAIN_FORM_SUBMIT).click();
        // After saving, the app returns to the owner details page.
        page.locator("h2:text-is('Owner Information')")
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        waitForLoadingSpinner();
    }
}
