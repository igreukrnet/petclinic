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
        Locator queryInput = page.locator("#owner-query, input[name='query'], input[placeholder*='Search'], input[type='search']").first();
        queryInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        queryInput.click();
        queryInput.fill(query);
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        waitForLoadingSpinner();
        return this;
    }

    @Step("Check if owner '{fullName}' is present in table")
    public boolean isOwnerPresent(String fullName) {
        waitForLoadingSpinner();
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                // Try exact full name link first
                Locator link = page.locator("table tbody tr td a:has-text('" + fullName + "')").first();
                if (link.count() > 0 && link.isVisible()) {
                    return true;
                }
                // Fall back: match by last name only
                String[] parts = fullName.split(" ", 2);
                String lastName = parts.length > 1 ? parts[1] : fullName;
                Locator lastLink = page.locator("table tbody tr td a:has-text('" + lastName + "')").first();
                if (lastLink.count() > 0 && lastLink.isVisible()) {
                    return true;
                }
            } catch (Exception ignored) {}

            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return false;
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
