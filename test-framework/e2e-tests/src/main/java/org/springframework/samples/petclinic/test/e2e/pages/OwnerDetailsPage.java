package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class OwnerDetailsPage extends BasePage {

    public OwnerDetailsPage(Page page) {
        super(page);
    }

    @Step("Open Owner Details page for ID: {ownerId}")
    public OwnerDetailsPage open(int ownerId) {
        navigate("/owners/details/" + ownerId);
        return this;
    }

    @Step("Get owner full name")
    public String getOwnerName() {
        waitForLoadingSpinner();
        com.microsoft.playwright.Locator locator = page.locator("table tr:has(th:has-text('Name')) td, table tr:has(td:has-text('Name')) td:nth-child(2)").first();
        locator.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));
        return locator.textContent().trim();
    }

    @Step("Get owner address")
    public String getAddress() {
        waitForLoadingSpinner();
        com.microsoft.playwright.Locator locator = page.locator("table tr:has(th:has-text('Address')) td, table tr:has(td:has-text('Address')) td:nth-child(2)").first();
        locator.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));
        // In AngularJS, data binds asynchronously; wait until text is populated
        for (int i = 0; i < 20; i++) {
            String text = locator.textContent().trim();
            if (!text.isEmpty()) {
                return text;
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return locator.textContent().trim();
    }

    @Step("Get owner city")
    public String getCity() {
        waitForLoadingSpinner();
        com.microsoft.playwright.Locator locator = page.locator("table tr:has(th:has-text('City')) td, table tr:has(td:has-text('City')) td:nth-child(2)").first();
        locator.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));
        for (int i = 0; i < 20; i++) {
            String text = locator.textContent().trim();
            if (!text.isEmpty()) {
                return text;
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return locator.textContent().trim();
    }

    @Step("Get owner telephone")
    public String getTelephone() {
        waitForLoadingSpinner();
        com.microsoft.playwright.Locator locator = page.locator("table tr:has(th:has-text('Telephone')) td, table tr:has(td:has-text('Telephone')) td:nth-child(2)").first();
        locator.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));
        for (int i = 0; i < 20; i++) {
            String text = locator.textContent().trim();
            if (!text.isEmpty()) {
                return text;
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return locator.textContent().trim();
    }

    @Step("Click 'Edit Owner'")
    public OwnerFormPage clickEditOwner() {
        page.locator("a:has-text('Edit Owner')").click();
        waitForLoadingSpinner();
        return new OwnerFormPage(page);
    }

    @Step("Click 'Add New Pet'")
    public PetFormPage clickAddNewPet() {
        page.locator("a:has-text('Add New Pet')").click();
        waitForLoadingSpinner();
        return new PetFormPage(page);
    }

    @Step("Check if pet '{petName}' is listed")
    public boolean hasPet(String petName) {
        waitForLoadingSpinner();
        try {
            page.locator("dd:has-text('" + petName + "')").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Click 'Add Visit' for pet")
    public VisitsPage clickAddVisit() {
        hideOverlays();
        page.locator("a:has-text('Add Visit')").first().click(new com.microsoft.playwright.Locator.ClickOptions().setForce(true));
        waitForLoadingSpinner();
        return new VisitsPage(page);
    }

    @Step("Check if visit with description '{description}' is listed")
    public boolean hasVisitDescription(String description) {
        waitForLoadingSpinner();
        try {
            page.locator("td:has-text('" + description + "')").first().waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(OwnerDetailsPage.class).warn("Visit description not found: {}. Exception: {}", description, e.toString());
            return false;
        }
    }
}
