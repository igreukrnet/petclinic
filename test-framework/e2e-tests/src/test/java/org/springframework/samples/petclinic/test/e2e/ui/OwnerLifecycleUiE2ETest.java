package org.springframework.samples.petclinic.test.e2e.ui;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;
import org.springframework.samples.petclinic.test.e2e.pages.OwnerDetailsPage;
import org.springframework.samples.petclinic.test.e2e.pages.OwnerFormPage;
import org.springframework.samples.petclinic.test.e2e.pages.OwnersPage;
import org.springframework.samples.petclinic.test.e2e.pages.PetFormPage;
import org.springframework.samples.petclinic.test.e2e.pages.VisitsPage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("UI Owner & Pet Lifecycle")
public class OwnerLifecycleUiE2ETest extends BaseE2ETest {

    @Test
    @Story("Complete user flow: Owner -> Pet -> Visit with SQL verification")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Create owner via UI, add pet, add visit, and verify data consistency directly in MySQL via JDBI")
    @Description("End-to-end user scenario testing Vue.js UI forms, routing, gateway aggregation and database state")
    void shouldCompleteFullOwnerPetVisitLifecycleViaUiAndVerifyInDatabase() {
        // Step 1: Generate test data
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        PetRequest petReq = TestDataGenerator.generatePetRequest(1); // cat
        VisitRequest visitReq = TestDataGenerator.generateVisitRequest();

        String fullName = ownerReq.firstName() + " " + ownerReq.lastName();

        // Step 2: Register owner via UI
        OwnerFormPage ownerFormPage = new OwnerFormPage(page).openNew();
        ownerFormPage.fillForm(ownerReq).submit();

        // Step 3: Find owner in Owners table
        OwnersPage ownersPage = new OwnersPage(page);
        ownersPage.search(ownerReq.lastName());
        assertThat(ownersPage.isOwnerPresent(fullName)).isTrue();

        // Step 4: Open owner details
        OwnerDetailsPage detailsPage = ownersPage.clickOwner(fullName);
        assertThat(detailsPage.getOwnerName()).contains(ownerReq.firstName(), ownerReq.lastName());
        assertThat(detailsPage.getAddress()).isEqualTo(ownerReq.address());
        assertThat(detailsPage.getCity()).isEqualTo(ownerReq.city());
        assertThat(detailsPage.getTelephone()).isEqualTo(ownerReq.telephone());

        // Step 5: Add pet via UI
        PetFormPage petFormPage = detailsPage.clickAddNewPet();
        petFormPage.fillForm(petReq).submit();

        // Step 6: Verify pet appears on owner details
        assertThat(detailsPage.hasPet(petReq.name())).isTrue();

        // Step 7: Add visit via UI
        VisitsPage visitsPage = detailsPage.clickAddVisit();
        visitsPage.fillForm(visitReq).submit();

        // Step 8: Verify visit on owner details
        assertThat(detailsPage.hasVisitDescription(visitReq.description())).isTrue();

        // Step 9: TRIPLE VERIFICATION: Direct SQL verification via JDBI
        Optional<OwnerDto> dbOwner = ownerDao.findByTelephone(ownerReq.telephone());
        assertThat(dbOwner).isPresent();
        OwnerDto owner = dbOwner.get();
        assertThat(owner.firstName()).isEqualTo(ownerReq.firstName());
        assertThat(owner.lastName()).isEqualTo(ownerReq.lastName());
        assertThat(owner.address()).isEqualTo(ownerReq.address());
        assertThat(owner.city()).isEqualTo(ownerReq.city());

        List<PetDto> dbPets = petDao.findByOwnerId(owner.id());
        assertThat(dbPets).isNotEmpty();
        PetDto matchedPet = dbPets.stream()
            .filter(p -> p.name().equals(petReq.name()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Pet not found in database: " + petReq.name()));
        assertThat(matchedPet.birthDate()).isEqualTo(petReq.birthDate());

        List<VisitDto> dbVisits = visitDao.findByPetId(matchedPet.id());
        assertThat(dbVisits).isNotEmpty();
        assertThat(dbVisits)
            .extracting(VisitDto::description)
            .contains(visitReq.description());

        // Cleanup
        deleteOwnerCascade(owner.id());
    }
}
