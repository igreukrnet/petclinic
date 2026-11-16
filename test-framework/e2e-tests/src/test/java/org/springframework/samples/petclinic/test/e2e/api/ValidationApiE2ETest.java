package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.util.List;
import java.util.stream.Stream;

@Epic("E2E Tests")
@Feature("API validation and error responses")
public class ValidationApiE2ETest extends BaseE2ETest {

    static Stream<Arguments> invalidOwners() {
        return Stream.of(
            Arguments.of("blank first name", new OwnerRequest(" ", "Lee", "1 Main St", "Springfield", "1234567890")),
            Arguments.of("missing last name", new OwnerRequest("Ann", null, "1 Main St", "Springfield", "1234567890")),
            Arguments.of("blank address", new OwnerRequest("Ann", "Lee", "", "Springfield", "1234567890")),
            Arguments.of("telephone with letters", new OwnerRequest("Ann", "Lee", "1 Main St", "Springfield", "12ab567890")),
            Arguments.of("telephone longer than 12 digits", new OwnerRequest("Ann", "Lee", "1 Main St", "Springfield", "1234567890123")),
            Arguments.of("empty telephone", new OwnerRequest("Ann", "Lee", "1 Main St", "Springfield", ""))
        );
    }

    @ParameterizedTest(name = "rejects owner with {0}")
    @MethodSource("invalidOwners")
    @Story("Owner validation")
    @DisplayName("Owner with an invalid field is rejected with 400")
    void shouldRejectInvalidOwnerWithBadRequest(String caseName, OwnerRequest request) {
        petClinicClient.createOwner(request).then().statusCode(400);
    }

    @ParameterizedTest(name = "rejects owner id {0}")
    @ValueSource(ints = {0, -1})
    @Story("Path parameter validation")
    @DisplayName("Owner id below 1 is rejected with 400")
    void shouldRejectNonPositiveOwnerIdWithBadRequest(int ownerId) {
        petClinicClient.getOwner(ownerId).then().statusCode(400);
    }

    @Test
    @Story("Missing resources")
    @DisplayName("Unknown owner gives 404 for read, update and add pet")
    void shouldReturn404ForUnknownOwner() {
        OwnerRequest request = TestDataGenerator.generateOwnerRequest();

        petClinicClient.getOwner(99999).then().statusCode(404);
        petClinicClient.updateOwner(99999, request).then().statusCode(404);
        petClinicClient.addPet(99999, new PetRequest("Rex", "2020-01-01", 2)).then().statusCode(404);
    }

    @Test
    @Story("Missing resources")
    @DisplayName("Unknown pet gives 404 for read and update")
    void shouldReturn404ForUnknownPet() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.getPet(ownerId, 99999).then().statusCode(404);
        petClinicClient.updatePet(ownerId, 99999, new PetRequest("Rex", "2020-01-01", 2)).then().statusCode(404);
    }

    @Test
    @Story("Pet validation")
    @DisplayName("Pet with a future birth date is rejected with 400")
    void shouldRejectPetWithFutureBirthDate() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.addPet(ownerId, new PetRequest("Future", "2099-01-01", 1)).then().statusCode(400);
    }

    @Test
    @Story("Pet validation")
    @DisplayName("Pet with a blank name is rejected with 400")
    void shouldRejectPetWithBlankName() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.addPet(ownerId, new PetRequest("", "2020-01-01", 1)).then().statusCode(400);
    }

    @Test
    @Story("Pet validation")
    @DisplayName("Pet with a birth date that is not yyyy-MM-dd is rejected with 400")
    void shouldRejectPetWithUnparseableBirthDate() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        petClinicClient.addPet(ownerId, new PetRequest("Bad", "07.09.2000", 1)).then().statusCode(400);
    }

    @Test
    @Story("Visit validation")
    @DisplayName("Visit with a date that is not yyyy-MM-dd is rejected with 400")
    void shouldRejectVisitWithUnparseableDate() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, TestDataGenerator.generatePetRequest(1));

        petClinicClient.addVisit(ownerId, petId, new VisitRequest("nope", "Checkup")).then().statusCode(400);
    }

    @Test
    @Story("Visit validation")
    @DisplayName("Visits query without any petId is rejected with 400")
    void shouldRejectVisitQueryWithoutPetIds() {
        petClinicClient.getVisitsForPets(List.of()).then().statusCode(400);
    }
}
