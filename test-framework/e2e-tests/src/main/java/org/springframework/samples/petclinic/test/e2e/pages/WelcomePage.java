package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;

public class WelcomePage extends BasePage {

    public WelcomePage(Page page) {
        super(page);
    }

    @Step("Open Welcome page")
    public WelcomePage open() {
        clickNavigation("home page");
        return this;
    }
}
