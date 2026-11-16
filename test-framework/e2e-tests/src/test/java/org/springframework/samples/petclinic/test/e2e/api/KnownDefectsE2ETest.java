package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for application defects that are confirmed on the running stack.
 * <p>
 * They assert the correct behaviour, so they FAIL until the application is fixed. Do not change the expected
 * status to match the current behaviour. Run the rest of the suite without them with
 * {@code -Dgroups=!known-defect}.
 */
@Tag("known-defect")
@Epic("E2E Tests")
@Feature("Known application defects")
public class KnownDefectsE2ETest extends BaseE2ETest {

    @Test
    @Story("Visit to a missing pet")
    @DisplayName("Adding a visit to a pet that does not exist returns 404, not 500")
    void shouldReturn404WhenVisitIsAddedToMissingPet() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.addVisit(ownerId, 99999, new VisitRequest("2026-01-01", "Checkup"))
            .then().statusCode(404);
    }

    @Test
    @Story("Pet with unknown type")
    @DisplayName("Adding a pet with a type that does not exist returns 400 and creates no pet")
    void shouldRejectPetWithUnknownTypeAndCreateNothing() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.addPet(ownerId, new PetRequest("Ghost", "2020-01-01", 99))
            .then().statusCode(400);
        assertThat(petDao.findByOwnerId(ownerId)).isEmpty();
    }
}
