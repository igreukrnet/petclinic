package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
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

    /**
     * The search box filters on the client side across all owner fields, not only last name.
     * Results are checked by the methods below, which wait for the filtered rows.
     */
    @Step("Search owners by query: {query}")
    public OwnersPage search(String query) {
        waitForLoadingSpinner();
        Locator queryInput = page.locator("#owner-query");
        queryInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        queryInput.fill(query);
        return this;
    }

    /**
     * Owners are identified by telephone: it is unique in the test data, while names are not.
     * Returns false only when the row does not appear within 5 s.
     */
    @Step("Check if owner with telephone '{telephone}' is listed")
    public boolean isOwnerPresent(String telephone) {
        waitForLoadingSpinner();
        try {
            ownerRow(telephone).waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
            return true;
        } catch (TimeoutError e) {
            return false;
        }
    }

    @Step("Click on owner with telephone: {telephone}")
    public OwnerDetailsPage clickOwner(String telephone) {
        waitForLoadingSpinner();
        ownerRow(telephone).locator("td a").first().click();
        waitForLoadingSpinner();
        return new OwnerDetailsPage(page);
    }

    public Locator getOwnersRows() {
        return page.locator("table tbody tr");
    }

    private Locator ownerRow(String telephone) {
        return page.locator("table tbody tr")
            .filter(new Locator.FilterOptions().setHasText(telephone))
            .first();
    }
}
