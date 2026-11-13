package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

public class VisitsPage extends BasePage {

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
        if (request.date() != null) {
            page.locator("#visit-date").fill(request.date());
        }
        if (request.description() != null) {
            page.locator("#visit-description").fill(request.description());
        }
        return this;
    }

    @Step("Submit visit form")
    public void submit() {
        page.locator("button.btn-primary[type='submit']").click();
        waitForLoadingSpinner();
    }
}
