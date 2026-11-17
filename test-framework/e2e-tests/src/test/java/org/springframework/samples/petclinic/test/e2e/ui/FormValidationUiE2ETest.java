package org.springframework.samples.petclinic.test.e2e.ui;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.e2e.BaseUiE2ETest;
import org.springframework.samples.petclinic.test.e2e.pages.OwnerFormPage;
import org.springframework.samples.petclinic.test.e2e.pages.PetFormPage;
import org.springframework.samples.petclinic.test.e2e.pages.VisitsPage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the forms do with invalid input. The browser's own validation is only observed: the tests never
 * switch it off, never remove attributes and never set values with scripts. Each test also checks that
 * nothing was sent to the server and nothing reached the database.
 * <p>
 * Forms are opened by clicking through the application. The visit form is reachable only by the Add Visit
 * link, which the chat box covers (issue #1), so the two visit tests are tagged {@code known-defect}.
 */
@Epic("E2E Tests")
@Feature("UI form validation")
@Tag("ui")
@Tag("negative")
class FormValidationUiE2ETest extends BaseUiE2ETest {

    private static final List<String> OWNER_FIELDS = List.of(
        OwnerFormPage.FIRST_NAME, OwnerFormPage.LAST_NAME, OwnerFormPage.ADDRESS, OwnerFormPage.CITY, OwnerFormPage.TELEPHONE);

    // ---- owner form ----

    @Test
    @Story("Register owner")
    @DisplayName("An empty owner form is not submitted: every field says it is required")
    void emptyOwnerFormIsNotSubmitted() {
        OwnerFormPage form = new OwnerFormPage(page).openNew();

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/new");
        for (String field : OWNER_FIELDS) {
            assertThat(form.invalidReason(field)).as("validity of " + field).isEqualTo("valueMissing");
            assertThat(form.validationMessage(field)).as("message of " + field).isNotBlank();
        }
        assertThat(form.writeRequestsSent()).isEmpty();
    }

    @ParameterizedTest(name = "telephone \"{0}\" is not accepted")
    @ValueSource(strings = {"abc", "12-34", "12 34"})
    @Story("Register owner")
    @DisplayName("A telephone that is not made of digits only is not submitted")
    void invalidTelephoneIsNotSubmitted(String telephone) {
        OwnerRequest owner = TestDataGenerator.generateOwnerRequest();
        OwnerFormPage form = new OwnerFormPage(page).openNew().fillForm(owner);
        form.fillField(OwnerFormPage.TELEPHONE, telephone);

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/new");
        assertThat(form.invalidReason(OwnerFormPage.TELEPHONE)).isEqualTo("patternMismatch");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(ownerDao.findByLastName(owner.lastName())).isEmpty();
    }

    @Test
    @Story("Register owner")
    @DisplayName("Typing into the telephone field stops at 12 characters")
    void telephoneFieldStopsAtTwelveCharacters() {
        OwnerFormPage form = new OwnerFormPage(page).openNew();

        form.typeInto(OwnerFormPage.TELEPHONE, "1234567890123456");

        assertThat(form.valueOf(OwnerFormPage.TELEPHONE)).isEqualTo("123456789012");
    }

    @Test
    @Story("Register owner")
    @DisplayName("A first name of only spaces does not create an owner, and the user is told")
    void blankFirstNameDoesNotCreateOwner() {
        OwnerRequest owner = TestDataGenerator.generateOwnerRequest();
        OwnerFormPage form = new OwnerFormPage(page).openNew().fillForm(owner);
        form.fillField(OwnerFormPage.FIRST_NAME, "   ");

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/new");
        assertThat(ownerDao.findByLastName(owner.lastName())).as("owners saved with a blank first name").isEmpty();
        // The user must learn why nothing happened: either the browser rejects the field
        // or the application shows the error it got from the server.
        boolean told = !form.invalidReason(OwnerFormPage.FIRST_NAME).isEmpty() || form.isErrorAlertShown();
        assertThat(told).as("the form shows no message about the blank first name").isTrue();
    }

    @Test
    @Story("Edit owner")
    @DisplayName("Clearing a required field on the edit form is not submitted and keeps the stored owner")
    void clearedRequiredFieldIsNotSubmittedOnEdit() {
        OwnerRequest owner = TestDataGenerator.generateOwnerRequest();
        int ownerId = createOwnerViaApi(owner);
        OwnerFormPage form = new OwnerFormPage(page).openEdit(ownerId);
        form.fillField(OwnerFormPage.LAST_NAME, "");

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/" + ownerId + "/edit");
        assertThat(form.invalidReason(OwnerFormPage.LAST_NAME)).isEqualTo("valueMissing");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(ownerDao.findById(ownerId).orElseThrow().lastName()).isEqualTo(owner.lastName());
    }

    @Test
    @Story("Edit owner")
    @DisplayName("An invalid telephone on the edit form is not submitted and keeps the stored owner")
    void invalidTelephoneIsNotSubmittedOnEdit() {
        OwnerRequest owner = TestDataGenerator.generateOwnerRequest();
        int ownerId = createOwnerViaApi(owner);
        OwnerFormPage form = new OwnerFormPage(page).openEdit(ownerId);
        form.fillField(OwnerFormPage.TELEPHONE, "not-a-number");

        form.submitExpectingRefusal();

        assertThat(form.invalidReason(OwnerFormPage.TELEPHONE)).isEqualTo("patternMismatch");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(ownerDao.findById(ownerId).orElseThrow().telephone()).isEqualTo(owner.telephone());
    }

    // ---- pet form ----

    @Test
    @Story("Add pet")
    @DisplayName("A pet without a name is not submitted")
    void petWithoutNameIsNotSubmitted() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        PetFormPage form = new PetFormPage(page).openNew(ownerId);
        form.fillField(PetFormPage.BIRTH_DATE, LocalDate.now().minusYears(2).toString());

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/" + ownerId + "/new-pet");
        assertThat(form.invalidReason(PetFormPage.NAME)).isEqualTo("valueMissing");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(petDao.findByOwnerId(ownerId)).isEmpty();
    }

    @Test
    @Story("Add pet")
    @DisplayName("A pet without a birth date is not submitted")
    void petWithoutBirthDateIsNotSubmitted() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        PetFormPage form = new PetFormPage(page).openNew(ownerId);
        form.fillField(PetFormPage.NAME, "Rex");

        form.submitExpectingRefusal();

        assertThat(form.invalidReason(PetFormPage.BIRTH_DATE)).isEqualTo("valueMissing");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(petDao.findByOwnerId(ownerId)).isEmpty();
    }

    @Test
    @Story("Add pet")
    @DisplayName("A birth date in the future is refused: the user sees the error and no pet is saved")
    void futureBirthDateIsRefused() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        PetRequest pet = new PetRequest("Future", LocalDate.now().plusYears(1).toString(), 1);
        PetFormPage form = new PetFormPage(page).openNew(ownerId);
        form.fillForm(pet);

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/" + ownerId + "/new-pet");
        assertThat(form.errorAlertText()).isNotBlank();
        assertThat(petDao.findByOwnerId(ownerId)).isEmpty();
    }

    // ---- visit form ----

    @Tag("known-defect")
    @Test
    @Story("Add visit")
    @DisplayName("Issue #1: a visit without a description is not submitted (the form cannot be reached: the chat box covers the Add Visit link)")
    void visitWithoutDescriptionIsNotSubmitted() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));
        VisitsPage form = new VisitsPage(page).open(ownerId, petId);

        form.submitExpectingRefusal();

        assertThat(form.currentPath()).isEqualTo("/owners/" + ownerId + "/pets/" + petId + "/visits");
        assertThat(form.invalidReason(VisitsPage.DESCRIPTION)).isEqualTo("valueMissing");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(visitDao.findByPetId(petId)).isEmpty();
    }

    @Tag("known-defect")
    @Test
    @Story("Add visit")
    @DisplayName("Issue #1: a visit without a date is not submitted (the form cannot be reached: the chat box covers the Add Visit link)")
    void visitWithoutDateIsNotSubmitted() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));
        VisitsPage form = new VisitsPage(page).open(ownerId, petId);
        form.fillField(VisitsPage.DATE, "");
        form.fillField(VisitsPage.DESCRIPTION, "checkup");

        form.submitExpectingRefusal();

        assertThat(form.invalidReason(VisitsPage.DATE)).isEqualTo("valueMissing");
        assertThat(form.writeRequestsSent()).isEmpty();
        assertThat(visitDao.findByPetId(petId)).isEmpty();
    }
}
