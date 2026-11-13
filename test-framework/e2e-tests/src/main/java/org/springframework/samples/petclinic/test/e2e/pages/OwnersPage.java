package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class OwnersPage extends BasePage {

    public OwnersPage(Page page) {
        super(page);
    }

    @Step("Open Owners list page")
    public OwnersPage open() {
        navigate("/owners");
        return this;
    }

    @Step("Search owners by query: {query}")
    public OwnersPage search(String query) {
        page.locator("#owner-query").fill(query);
        return this;
    }

    @Step("Check if owner '{fullName}' is present in table")
    public boolean isOwnerPresent(String fullName) {
        waitForLoadingSpinner();
        try {
            page.locator("table tbody tr td a:has-text('" + fullName + "')").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Click on owner details: {fullName}")
    public OwnerDetailsPage clickOwner(String fullName) {
        waitForLoadingSpinner();
        page.locator("table tbody tr td a:has-text('" + fullName + "')").click();
        waitForLoadingSpinner();
        return new OwnerDetailsPage(page);
    }

    public Locator getOwnersRows() {
        return page.locator("table tbody tr");
    }
}
