package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import io.qameta.allure.Step;
import org.slf4j.LoggerFactory;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

    @Step("Check if pet '{petName}' is listed")
    public boolean hasPet(String petName) {
        waitForLoadingSpinner();
        return isVisibleWithin(page.locator("dd").filter(new Locator.FilterOptions().setHasText(petName)).first());
    }

    @Step("Click 'Add Visit' for pet")
    public VisitsPage clickAddVisit() {
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add Visit")).first().click();
        waitForLoadingSpinner();
        return new VisitsPage(page);
    }

    @Step("Check if visit with description '{description}' is listed")
    public boolean hasVisitDescription(String description) {
        waitForLoadingSpinner();
        Locator cell = page.locator("td").filter(new Locator.FilterOptions().setHasText(description)).first();
        boolean visible = isVisibleWithin(cell);
        if (!visible) {
            LoggerFactory.getLogger(OwnerDetailsPage.class).warn("Visit description not found: {}", description);
        }
        return visible;
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

    private boolean isVisibleWithin(Locator locator) {
        try {
            locator.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(5000));
            return true;
        } catch (TimeoutError e) {
            return false;
        }
    }
}
