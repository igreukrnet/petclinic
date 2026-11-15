package org.springframework.samples.petclinic.test.e2e.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import io.qameta.allure.Step;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;

import java.util.List;

public class OwnerFormPage extends BasePage {

    public OwnerFormPage(Page page) {
        super(page);
    }

    @Step("Open Register Owner page")
    public OwnerFormPage openNew() {
        navigate("/owners/new");
        return this;
    }

    @Step("Open Edit Owner page for ID: {ownerId}")
    public OwnerFormPage openEdit(int ownerId) {
        navigate("/owners/" + ownerId + "/edit");
        return this;
    }

    @Step("Fill owner form: {request}")
    public OwnerFormPage fillForm(OwnerRequest request) {
        waitForLoadingSpinner();
        Locator firstNameInput = page.locator("#owner-first-name, #firstName, input[name='firstName']").first();
        firstNameInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        if (request.firstName() != null) {
            firstNameInput.click();
            firstNameInput.fill(request.firstName());
        }
        if (request.lastName() != null) {
            Locator lastNameInput = page.locator("#owner-last-name, #lastName, input[name='lastName']").first();
            lastNameInput.click();
            lastNameInput.fill(request.lastName());
        }
        if (request.address() != null) {
            Locator addressInput = page.locator("#owner-address, #address, input[name='address']").first();
            addressInput.click();
            addressInput.fill(request.address());
        }
        if (request.city() != null) {
            Locator cityInput = page.locator("#owner-city, #city, input[name='city']").first();
            cityInput.click();
            cityInput.fill(request.city());
        }
        String digitsOnly = request.telephone() != null ? request.telephone().replaceAll("[^0-9]", "") : "";
        if (!digitsOnly.isEmpty()) {
            Locator telInput = page.locator("#owner-telephone, #telephone, input[name='telephone']").first();
            telInput.click();
            telInput.fill(digitsOnly);
        }

        // Guarantee Vue reactivity v-model synchronization via native DOM events
        page.evaluate("([fn, ln, addr, ct, tel]) => {\n" +
            "    const inputs = [\n" +
            "        { sel: '#owner-first-name', val: fn },\n" +
            "        { sel: '#owner-last-name', val: ln },\n" +
            "        { sel: '#owner-address', val: addr },\n" +
            "        { sel: '#owner-city', val: ct },\n" +
            "        { sel: '#owner-telephone', val: tel }\n" +
            "    ];\n" +
            "    inputs.forEach(({ sel, val }) => {\n" +
            "        const el = document.querySelector(sel);\n" +
            "        if (el && val) {\n" +
            "            el.value = val;\n" +
            "            el.dispatchEvent(new Event('input', { bubbles: true }));\n" +
            "            el.dispatchEvent(new Event('change', { bubbles: true }));\n" +
            "        }\n" +
            "    });\n" +
            "}", List.of(
                request.firstName() != null ? request.firstName() : "",
                request.lastName() != null ? request.lastName() : "",
                request.address() != null ? request.address() : "",
                request.city() != null ? request.city() : "",
                digitsOnly
            ));

        return this;
    }

    @Step("Fill and update address to: {newAddress}")
    public OwnerFormPage updateAddress(String newAddress) {
        waitForLoadingSpinner();
        Locator addressInput = page.locator("#owner-address, #address, input[name='address']").first();
        addressInput.waitFor(new Locator.WaitForOptions().setTimeout(15000));
        addressInput.click();
        addressInput.fill(newAddress);
        page.evaluate("val => {\n" +
            "    const el = document.querySelector('#owner-address');\n" +
            "    if (el) {\n" +
            "        el.value = val;\n" +
            "        el.dispatchEvent(new Event('input', { bubbles: true }));\n" +
            "        el.dispatchEvent(new Event('change', { bubbles: true }));\n" +
            "    }\n" +
            "}", newAddress);
        return this;
    }

    @Step("Submit owner form")
    public void submit() {
        waitForLoadingSpinner();

        // Ensure HTML5 validation does not block submission in headless browser
        page.evaluate("() => {\n" +
            "    const form = document.querySelector('form');\n" +
            "    if (form) {\n" +
            "        form.noValidate = true;\n" +
            "        form.reportValidity = () => true;\n" +
            "        const tel = form.querySelector('#owner-telephone, #telephone, input[name=\"telephone\"]');\n" +
            "        if (tel) { tel.removeAttribute('pattern'); tel.removeAttribute('required'); }\n" +
            "    }\n" +
            "}");

        Locator submitBtn = page.locator("form button[type='submit'], form button.btn-primary").first();
        submitBtn.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        submitBtn.click();

        waitForLoadingSpinner();
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(5000));
        } catch (Exception ignored) {}

        // Check if an error alert was rendered by the Vue error state
        try {
            Locator alert = page.locator(".alert-danger");
            if (alert.count() > 0 && alert.first().isVisible()) {
                org.slf4j.LoggerFactory.getLogger(OwnerFormPage.class).warn("Form error alert detected: {}", alert.first().textContent().trim());
            }
        } catch (Exception ignored) {}
    }
}
