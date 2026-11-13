package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class WelcomePage extends BasePage {

    public WelcomePage(Page page) {
        super(page);
    }

    @Step("Open Welcome page")
    public WelcomePage open() {
        navigate("/");
        return this;
    }

    @Step("Check if welcome title is visible")
    public boolean isLoaded() {
        return page.locator("h2:has-text('Welcome')").isVisible()
            || page.locator("img.img-responsive").isVisible();
    }
}
