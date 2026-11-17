package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OwnersPage extends BasePage {

    public OwnersPage(Page page) {
        super(page);
    }

    /**
     * Opens the list from the navigation bar. It goes through the home page first: the list is loaded
     * when the page is entered, so entering it again shows owners created since the last visit.
     */
    @Step("Open Owners list page")
    public OwnersPage open() {
        clickNavigation("home page");
        clickNavigation("find owners");
        assertThat(page.locator("h2:text-is('Owners')")).isVisible();
        return this;
    }

    /** The search box filters on the client side across all owner fields, not only last name. */
    @Step("Search owners by query: {query}")
    public OwnersPage search(String query) {
        page.locator("#owner-query").fill(query);
        return this;
    }

    /**
     * Owners are identified by telephone: it is unique in the test data, while names are not.
     * Fails with the message of the Playwright assertion when the row does not appear.
     */
    @Step("Check that the owner with telephone '{telephone}' is listed")
    public OwnersPage assertOwnerListed(String telephone) {
        assertThat(ownerRow(telephone)).isVisible();
        return this;
    }

    @Step("Click on owner with telephone: {telephone}")
    public OwnerDetailsPage clickOwner(String telephone) {
        ownerRow(telephone).locator("td a").first().click();
        waitForLoadingSpinner();
        return new OwnerDetailsPage(page);
    }

    /** Opens the details of an owner by the link in the list, which is how a user gets there. */
    @Step("Open owner {ownerId} from the list")
    public OwnerDetailsPage openOwner(int ownerId) {
        page.locator("table a[href='/owners/details/" + ownerId + "']").click();
        waitForLoadingSpinner();
        return new OwnerDetailsPage(page);
    }

    private Locator ownerRow(String telephone) {
        return page.locator("table tbody tr")
            .filter(new Locator.FilterOptions().setHasText(telephone))
            .first();
    }
}
