package org.springframework.samples.petclinic.test.e2e.hybrid;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;
import org.springframework.samples.petclinic.test.e2e.pages.OwnerDetailsPage;
import org.springframework.samples.petclinic.test.e2e.pages.OwnerFormPage;
import org.springframework.samples.petclinic.test.e2e.pages.OwnersPage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("Hybrid UI + API + SQL Consistency")
@Tag("smoke")
@Tag("hybrid")
public class HybridConsistencyE2ETest extends BaseE2ETest {

    @Test
    @Story("Cross-layer consistency (API Create -> UI Verify/Edit -> SQL Verify)")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Create owner via fast REST API, verify and update via UI, check consistency directly in MySQL")
    @Description("Tests real-world end-to-end multi-layer consistency between REST Assured, Playwright and JDBI 3")
    void shouldCreateViaApiModifyViaUiAndVerifyInDatabase() {
        // Step 1: Fast API setup
        OwnerRequest initialOwner = TestDataGenerator.generateOwnerRequest();
        Response createResponse = petClinicClient.createOwner(initialOwner);
        createResponse.then().statusCode(201);
        int ownerId = createResponse.jsonPath().getInt("id");
        trackOwner(ownerId);

        // Step 2: UI verification on Owners page
        OwnersPage ownersPage = new OwnersPage(page).open();
        ownersPage.search(initialOwner.telephone());
        assertThat(ownersPage.isOwnerPresent(initialOwner.telephone())).isTrue();

        // Step 3: Open details and click Edit Owner
        OwnerDetailsPage detailsPage = ownersPage.clickOwner(initialOwner.telephone());
        assertThat(detailsPage.getAddress()).isEqualTo(initialOwner.address());

        OwnerFormPage editPage = detailsPage.clickEditOwner();

        // Step 4: Update address via UI
        String updatedAddress = initialOwner.address() + " Suite 200";
        editPage.updateAddress(updatedAddress).submit();

        // Step 5: Verify updated address on UI
        assertThat(detailsPage.getAddress()).isEqualTo(updatedAddress);

        // Step 6: Direct SQL verification in MySQL via JDBI
        Optional<OwnerDto> dbOwner = ownerDao.findById(ownerId);
        assertThat(dbOwner).isPresent();
        assertThat(dbOwner.get().address()).isEqualTo(updatedAddress);
        assertThat(dbOwner.get().telephone()).isEqualTo(initialOwner.telephone());
    }
}
