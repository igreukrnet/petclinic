package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("Visits API")
@Tag("api")
public class VisitsApiE2ETest extends BaseE2ETest {

    @Test
    @Story("Visits per pet")
    @DisplayName("Visits of one pet do not include visits of another pet of the same owner")
    void shouldReturnVisitsOnlyForRequestedPet() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petA = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));
        int petB = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(2));
        addVisitViaApi(ownerId, petA, TestDataGenerator.generateVisitRequest());
        addVisitViaApi(ownerId, petA, TestDataGenerator.generateVisitRequest());
        addVisitViaApi(ownerId, petB, TestDataGenerator.generateVisitRequest());

        assertThat(petClinicClient.getVisitDtos(ownerId, petA))
            .hasSize(2)
            .extracting(VisitDto::petId)
            .containsOnly(petA);
        assertThat(petClinicClient.getVisitDtos(ownerId, petB))
            .hasSize(1)
            .extracting(VisitDto::petId)
            .containsOnly(petB);
    }

    @Test
    @Story("Visits per pet")
    @DisplayName("A pet without visits gets an empty list, not an error")
    void shouldReturnEmptyListForPetWithoutVisits() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));

        assertThat(petClinicClient.getVisitDtos(ownerId, petId)).isEmpty();
    }

    @Test
    @Story("Visits aggregation")
    @DisplayName("Visits query for several pets returns visits of all of them")
    void shouldAggregateVisitsOfSeveralPets() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petA = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));
        int petB = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(2));
        addVisitViaApi(ownerId, petA, TestDataGenerator.generateVisitRequest());
        addVisitViaApi(ownerId, petA, TestDataGenerator.generateVisitRequest());
        addVisitViaApi(ownerId, petB, TestDataGenerator.generateVisitRequest());

        var response = petClinicClient.getVisitsForPets(List.of(petA, petB));
        response.then().statusCode(200);
        List<Integer> petIds = response.jsonPath().getList("items.petId", Integer.class);

        assertThat(petIds).containsExactlyInAnyOrder(petA, petA, petB);
    }

    @Test
    @Story("Visit date")
    @DisplayName("Visit date sent as yyyy-MM-dd is returned as the same calendar date")
    void shouldReturnVisitDateAsSentCalendarDate() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));
        addVisitViaApi(ownerId, petId, new VisitRequest("2026-03-14", "Vaccination"));

        assertThat(petClinicClient.getVisitDtos(ownerId, petId))
            .singleElement()
            .satisfies(visit -> {
                assertThat(visit.date()).isEqualTo(LocalDate.of(2026, 3, 14));
                assertThat(visit.description()).isEqualTo("Vaccination");
            });
    }
}
