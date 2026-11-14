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
        waitForLoadingSpinner();
        Locator queryInput = page.locator("input[placeholder*='Search'], #owner-query, input[name='query'], input[type='search'], input[ng-model*='query']").first();
        if (queryInput.count() == 0) {
            navigate("/owners");
            queryInput = page.locator("input[placeholder*='Search'], #owner-query, input[name='query'], input[type='search'], input[ng-model*='query']").first();
        }
        queryInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        queryInput.click();
        queryInput.fill(query);
        queryInput.press("Enter");
        waitForLoadingSpinner();
        return this;
    }

    @Step("Check if owner '{fullName}' is present in table")
    public boolean isOwnerPresent(String fullName) {
        waitForLoadingSpinner();
        try {
            Locator link = page.locator("table tbody tr td a:has-text('" + fullName + "')").first();
            if (link.count() == 0) {
                // Try matching by first name or last name
                String[] parts = fullName.split(" ");
                if (parts.length > 1) {
                    link = page.locator("table tbody tr td a:has-text('" + parts[1] + "')").first();
                }
            }
            link.waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(10000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Step("Click on owner details: {fullName}")
    public OwnerDetailsPage clickOwner(String fullName) {
        waitForLoadingSpinner();
        Locator link = page.locator("table tbody tr td a:has-text('" + fullName + "')").first();
        if (link.count() == 0) {
            String[] parts = fullName.split(" ");
            if (parts.length > 1) {
                link = page.locator("table tbody tr td a:has-text('" + parts[1] + "')").first();
            }
        }
        link.click();
        waitForLoadingSpinner();
        return new OwnerDetailsPage(page);
    }

    public Locator getOwnersRows() {
        return page.locator("table tbody tr");
    }
}
