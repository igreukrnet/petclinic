package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class VetsPage extends BasePage {

    private static final String HEADING = "h2:text-is('Veterinarians')";

    public VetsPage(Page page) {
        super(page);
    }

    @Step("Open Veterinarians list page")
    public VetsPage open() {
        navigate("/vets");
        return this;
    }

    /** Opens the page by the navigation bar link, as a user does. */
    @Step("Open Veterinarians list page from the navigation bar")
    public VetsPage openFromNavigation() {
        page.locator("a.nav-link[title='veterinarians']").click();
        return this;
    }

    /** Waits for the list to be shown. Fails with the original assertion message if it never is. */
    private void waitUntilListed() {
        assertThat(page.locator(HEADING)).isVisible();
    }

    @Step("Get the veterinarians shown in the table")
    public List<VetRow> getVets() {
        waitUntilListed();
        return page.locator("table tbody tr").all().stream()
            .map(row -> new VetRow(
                row.locator("td").nth(0).innerText().trim(),
                row.locator("td").nth(1).innerText().trim()))
            .toList();
    }

    @Step("Get the title of the error box shown instead of the list")
    public String loadFailureTitle() {
        Locator box = page.locator(".alert-danger strong");
        assertThat(box).isVisible();
        return box.innerText().trim();
    }

    public boolean isTableShown() {
        return page.locator("table").count() > 0;
    }

    public String currentPath() {
        return java.net.URI.create(page.url()).getPath();
    }

    public Locator pageHeading() {
        return page.locator(HEADING);
    }

    /** A row of the table: the full name and the specialties text, which is "none" when there are none. */
    public record VetRow(String name, String specialties) {
    }
}
