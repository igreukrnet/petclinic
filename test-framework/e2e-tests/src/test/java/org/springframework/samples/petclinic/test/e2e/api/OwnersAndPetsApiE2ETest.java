package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("Owners and pets API")
public class OwnersAndPetsApiE2ETest extends BaseE2ETest {

    @Test
    @Story("Owner update")
    @DisplayName("Update owner address and city with PUT and read the change back")
    void shouldUpdateOwnerAndReadBackChanges() {
        OwnerRequest original = TestDataGenerator.generateOwnerRequest();
        int ownerId = createOwnerViaApi(original);

        OwnerRequest updated = new OwnerRequest(
            original.firstName(), original.lastName(), "42 New Road", "Shelbyville", original.telephone());
        petClinicClient.updateOwner(ownerId, updated).then().statusCode(204);

        OwnerDto owner = petClinicClient.getOwnerDto(ownerId);
        assertThat(owner.address()).isEqualTo("42 New Road");
        assertThat(owner.city()).isEqualTo("Shelbyville");
        assertThat(owner.telephone()).isEqualTo(original.telephone());
    }

    @Test
    @Story("Owner update")
    @DisplayName("Invalid update is rejected and leaves the stored owner unchanged")
    void shouldRejectInvalidUpdateAndKeepOwnerUnchanged() {
        OwnerRequest original = TestDataGenerator.generateOwnerRequest();
        int ownerId = createOwnerViaApi(original);

        OwnerRequest invalid = new OwnerRequest(
            original.firstName(), original.lastName(), "Changed Street", original.city(), "not-a-phone");
        petClinicClient.updateOwner(ownerId, invalid).then().statusCode(400);

        OwnerDto owner = petClinicClient.getOwnerDto(ownerId);
        assertThat(owner.address()).isEqualTo(original.address());
        assertThat(owner.telephone()).isEqualTo(original.telephone());
    }

    @Test
    @Story("Owner list")
    @DisplayName("A newly created owner appears in the list of all owners")
    void shouldListCreatedOwnerAmongAllOwners() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());

        assertThat(petClinicClient.getAllOwnerDtos())
            .extracting(OwnerDto::id)
            .contains(ownerId);
    }

    @Test
    @Story("Pet update")
    @DisplayName("Change pet type and birth date with PUT and read the change back as DTO")
    void shouldChangePetTypeAndBirthDate() {
        int ownerId = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petId = addPetViaApi(ownerId, new PetRequest("Tom", "2018-03-01", 1)); // cat

        petClinicClient.updatePet(ownerId, petId, new PetRequest("Tom", "2019-05-05", 4)) // snake
            .then().statusCode(204);

        PetDto pet = petClinicClient.getPetDto(ownerId, petId);
        assertThat(pet.name()).isEqualTo("Tom");
        assertThat(pet.type().name()).isEqualTo("snake");
        assertThat(pet.birthDate()).isEqualTo(LocalDate.of(2019, 5, 5));
    }

    @Test
    @Story("Pet isolation")
    @DisplayName("Pets are listed only under the owner they belong to")
    void shouldKeepPetsOfDifferentOwnersSeparate() {
        int ownerA = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int ownerB = createOwnerViaApi(TestDataGenerator.generateOwnerRequest());
        int petA = addPetViaApi(ownerA, TestDataGenerator.generatePetRequest(1));
        int petB = addPetViaApi(ownerB, TestDataGenerator.generatePetRequest(2));

        assertThat(petClinicClient.getOwnerDto(ownerA).pets())
            .extracting(PetDto::id)
            .containsExactly(petA);
        assertThat(petClinicClient.getOwnerDto(ownerB).pets())
            .extracting(PetDto::id)
            .containsExactly(petB);
    }
}
