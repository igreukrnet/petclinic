package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Epic("E2E Tests")
@Feature("Typed API client")
@Tag("api")
public class TypedClientE2ETest extends BaseE2ETest {

    @Test
    @Story("Pet read and update through the typed client")
    @DisplayName("Read a pet as PetDto, update it with PUT, and read the change back")
    @Description("Checks that the DTO carries the birth date as LocalDate and the type name, and that PUT /pets/{id} is applied")
    void shouldReadAndUpdatePetThroughTypedClient() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = petClinicClient.createOwner(ownerReq).then().statusCode(201).extract().jsonPath().getInt("id");
        trackOwner(ownerId);

        PetRequest petReq = TestDataGenerator.generatePetRequest(3); // lizard
        int petId = petClinicClient.addPet(ownerId, petReq).then().statusCode(201).extract().jsonPath().getInt("id");

        PetDto pet = petClinicClient.getPetDto(ownerId, petId);
        assertThat(pet.name()).isEqualTo(petReq.name());
        assertThat(pet.birthDate()).isEqualTo(LocalDate.parse(petReq.birthDate()));
        assertThat(pet.type().name()).isEqualTo("lizard");

        PetRequest renamed = new PetRequest(petReq.name() + "_v2", petReq.birthDate(), petReq.typeId());
        petClinicClient.updatePet(ownerId, petId, renamed).then().statusCode(204);

        assertThat(petClinicClient.getPetDto(ownerId, petId).name()).isEqualTo(renamed.name());
    }

    @Test
    @Story("Owner and reference data through the typed client")
    @DisplayName("Read owner, pet types and vets as DTOs and check their contents")
    void shouldReadOwnerAndReferenceDataThroughTypedClient() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = petClinicClient.createOwner(ownerReq).then().statusCode(201).extract().jsonPath().getInt("id");
        trackOwner(ownerId);

        OwnerDto owner = petClinicClient.getOwnerDto(ownerId);
        assertThat(owner.id()).isEqualTo(ownerId);
        assertThat(owner.telephone()).isEqualTo(ownerReq.telephone());
        assertThat(owner.pets()).isEmpty();

        assertThat(petClinicClient.getPetTypeDtos())
            .extracting(t -> t.name())
            .contains("cat", "dog", "lizard", "snake", "bird", "hamster");

        assertThat(petClinicClient.getVetDtos()).isNotEmpty();
    }

    @Test
    @Story("Typed client fails loudly on error answers")
    @DisplayName("A DTO read for a missing owner throws with the HTTP status instead of returning empty fields")
    @Description("Without this check the error body would be mapped to a DTO with null fields")
    void shouldFailWithStatusWhenOwnerIsMissing() {
        assertThatThrownBy(() -> petClinicClient.getOwnerDto(Integer.MAX_VALUE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HTTP 404");
    }
}
