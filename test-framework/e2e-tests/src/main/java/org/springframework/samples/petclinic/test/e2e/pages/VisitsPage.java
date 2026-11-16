package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

public class VisitsPage extends FormPage {

    public static final String DATE = "#visit-date";
    public static final String DESCRIPTION = "#visit-description";

    public VisitsPage(Page page) {
        super(page);
    }

    @Step("Open Visits page for owner {ownerId} and pet {petId}")
    public VisitsPage open(int ownerId, int petId) {
        navigate("/owners/" + ownerId + "/pets/" + petId + "/visits");
        return this;
    }

    @Step("Fill visit form: {request}")
    public VisitsPage fillForm(VisitRequest request) {
        waitForLoadingSpinner();
        page.locator(DATE).fill(request.date());
        page.locator(DESCRIPTION).fill(request.description());
        return this;
    }

    @Step("Submit visit form")
    public void submit() {
        page.locator(MAIN_FORM_SUBMIT).click();
        // After saving, the app returns to the owner details page.
        page.locator("h2:text-is('Owner Information')")
            .waitFor(new Locator.WaitForOptions().setTimeout(15000));
        waitForLoadingSpinner();
    }
}
