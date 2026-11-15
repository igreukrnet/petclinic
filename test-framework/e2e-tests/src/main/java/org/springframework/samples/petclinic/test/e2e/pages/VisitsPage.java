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
        com.microsoft.playwright.Locator dateInput = page.locator("#date, #visit-date, input[name='date']").first();
        dateInput.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(15000));
        if (request.date() != null) {
            dateInput.fill(request.date());
        }
        if (request.description() != null) {
            page.locator("#description, #visit-description, textarea[name='description'], input[name='description']").first().fill(request.description());
        }
        return this;
    }

    @Step("Submit visit form")
    public void submit() {
        page.locator("form button[type='submit'], button.btn-primary, button.btn-default").first().click();
        waitForLoadingSpinner();
    }
}
