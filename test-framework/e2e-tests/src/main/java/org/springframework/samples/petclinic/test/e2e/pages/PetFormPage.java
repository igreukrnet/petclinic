package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class PetFormPage extends FormPage {

    public static final String NAME = "#pet-name";
    public static final String BIRTH_DATE = "#pet-birth-date";
    public static final String TYPE = "#pet-type";

    public PetFormPage(Page page) {
        super(page);
    }

    @Step("Open New Pet form for owner ID: {ownerId}")
    public PetFormPage openNew(int ownerId) {
        new OwnerDetailsPage(page).open(ownerId);
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add New Pet")).click();
        waitForLoadingSpinner();
        assertThat(page.locator(NAME)).isVisible();
        return this;
    }

    @Step("Fill pet form: {request}")
    public PetFormPage fillForm(PetRequest request) {
        waitForLoadingSpinner();
        page.locator(NAME).fill(request.name());
        page.locator(BIRTH_DATE).fill(request.birthDate());
        page.locator(TYPE).selectOption(String.valueOf(request.typeId()));
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
