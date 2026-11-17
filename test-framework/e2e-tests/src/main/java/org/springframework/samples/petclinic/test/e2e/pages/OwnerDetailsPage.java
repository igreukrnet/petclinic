package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class OwnerDetailsPage extends BasePage {

    public OwnerDetailsPage(Page page) {
        super(page);
    }

    @Step("Open Owner Details page for ID: {ownerId}")
    public OwnerDetailsPage open(int ownerId) {
        new OwnersPage(page).open().openOwner(ownerId);
        waitUntilLoaded();
        return this;
    }

    /** Clicks the "Add Visit" link of one pet. */
    @Step("Click Add Visit for pet {petId} of owner {ownerId}")
    public void clickAddVisit(int ownerId, int petId) {
        page.locator("a[href='/owners/" + ownerId + "/pets/" + petId + "/visits']").click();
        waitForLoadingSpinner();
    }

    @Step("Get owner full name")
    public String getOwnerName() {
        return valueOf("Name");
    }

    @Step("Get owner address")
    public String getAddress() {
        return valueOf("Address");
    }

    @Step("Get owner city")
    public String getCity() {
        return valueOf("City");
    }

    @Step("Get owner telephone")
    public String getTelephone() {
        return valueOf("Telephone");
    }

    @Step("Click 'Edit Owner'")
    public OwnerFormPage clickEditOwner() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit Owner")).click();
        waitForLoadingSpinner();
        return new OwnerFormPage(page);
    }

    @Step("Click 'Add New Pet'")
    public PetFormPage clickAddNewPet() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add New Pet")).click();
        waitForLoadingSpinner();
        return new PetFormPage(page);
    }

    /**
     * The details are rendered together with the heading, so once the heading is visible the lists are complete.
     * Waiting for it fails with the original assertion message if the page never loads; the checks below
     * then read the page as it is and never wait for something that may not come.
     */
    private void waitUntilLoaded() {
        waitForLoadingSpinner();
        assertThat(page.locator("h2:text-is('Owner Information')")).isVisible();
    }

    @Step("Check if pet '{petName}' is listed")
    public boolean hasPet(String petName) {
        waitUntilLoaded();
        return page.locator("dd").filter(new Locator.FilterOptions().setHasText(petName)).count() > 0;
    }

    @Step("Click 'Add Visit' for pet")
    public VisitsPage clickAddVisit() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add Visit")).first().click();
        waitForLoadingSpinner();
        return new VisitsPage(page);
    }

    @Step("Check if visit with description '{description}' is listed")
    public boolean hasVisitDescription(String description) {
        waitUntilLoaded();
        return page.locator("td").filter(new Locator.FilterOptions().setHasText(description)).count() > 0;
    }

    /**
     * Reads a value from the owner details table by its row label.
     * The assertion waits until the value is rendered, so no manual polling is needed.
     */
    private String valueOf(String label) {
        waitForLoadingSpinner();
        Locator value = page.locator("table tr:has(th:text-is('" + label + "')) td").first();
        assertThat(value).not().isEmpty();
        return value.textContent().trim();
    }
}
