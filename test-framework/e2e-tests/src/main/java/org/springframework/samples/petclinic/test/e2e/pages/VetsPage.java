package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class VetsPage extends BasePage {

    public VetsPage(Page page) {
        super(page);
    }

    @Step("Open Veterinarians list page")
    public VetsPage open() {
        navigate("/vets");
        return this;
    }

    @Step("Get total number of veterinarians displayed")
    public int getVetsCount() {
        waitForLoadingSpinner();
        return page.locator("table tbody tr").count();
    }

    @Step("Check if vet '{fullName}' is listed")
    public boolean hasVet(String fullName) {
        waitForLoadingSpinner();
        try {
            page.locator("table tbody tr td:has-text('" + fullName + "')").waitFor(new com.microsoft.playwright.Locator.WaitForOptions().setTimeout(5000));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Locator getVetsRows() {
        return page.locator("table tbody tr");
    }
}
