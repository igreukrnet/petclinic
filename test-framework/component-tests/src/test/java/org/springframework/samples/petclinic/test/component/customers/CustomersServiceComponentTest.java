package org.springframework.samples.petclinic.test.component.customers;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Component Tests")
@Feature("Customers Service")
public class CustomersServiceComponentTest extends BaseComponentTest {

    @Test
    @Story("Owner persistence")
    @DisplayName("Insert and retrieve owner with exact field validation in Testcontainers MySQL")
    @Description("Verifies schema constraints, phone number validation and SQL mapping for owners table")
    void shouldInsertAndFindOwner() {
        OwnerRequest request = TestDataGenerator.generateOwnerRequest();

        int ownerId = ownerDao.insert(request);
        assertThat(ownerId).isPositive();

        Optional<OwnerDto> foundById = ownerDao.findById(ownerId);
        assertThat(foundById).isPresent();
        OwnerDto owner = foundById.get();
        assertThat(owner.firstName()).isEqualTo(request.firstName());
        assertThat(owner.lastName()).isEqualTo(request.lastName());
        assertThat(owner.address()).isEqualTo(request.address());
        assertThat(owner.city()).isEqualTo(request.city());
        assertThat(owner.telephone()).isEqualTo(request.telephone());

        Optional<OwnerDto> foundByPhone = ownerDao.findByTelephone(request.telephone());
        assertThat(foundByPhone).isPresent();
        assertThat(foundByPhone.get().id()).isEqualTo(ownerId);
    }

    @Test
    @Story("Pet persistence")
    @DisplayName("Insert pet for owner, query by ownerId and verify cascade deletion")
    @Description("Verifies foreign key relationship between owners and pets, and cascade deletion")
    void shouldInsertPetForOwnerAndCascadeDelete() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = ownerDao.insert(ownerReq);

        PetRequest petReq = TestDataGenerator.generatePetRequest(1); // cat
        int petId = petDao.insert(ownerId, petReq);
        assertThat(petId).isPositive();

        Optional<PetDto> foundPet = petDao.findById(petId);
        assertThat(foundPet).isPresent();
        assertThat(foundPet.get().name()).isEqualTo(petReq.name());
        assertThat(foundPet.get().birthDate()).isEqualTo(LocalDate.parse(petReq.birthDate()));
        assertThat(foundPet.get().type()).isNotNull();
        assertThat(foundPet.get().type().id()).isEqualTo(1);
        assertThat(foundPet.get().type().name()).isEqualTo("cat");

        List<PetDto> ownerPets = petDao.findByOwnerId(ownerId);
        assertThat(ownerPets).hasSize(1);
        assertThat(ownerPets.getFirst().id()).isEqualTo(petId);

        // Cascade delete verification
        ownerDao.deleteById(ownerId);
        assertThat(ownerDao.findById(ownerId)).isEmpty();
        assertThat(petDao.findById(petId)).isEmpty();
    }

    @Test
    @Story("Pet Types")
    @DisplayName("Validate standard pet types are pre-populated in database")
    @Description("Verifies initial seed types (cat, dog, lizard, snake, bird, hamster) exist")
    void shouldValidatePetTypesPreloaded() {
        List<PetTypeDto> types = petDao.findAllTypes();
        assertThat(types)
            .isNotEmpty()
            .extracting(PetTypeDto::name)
            .contains("cat", "dog", "lizard", "snake", "bird", "hamster");
    }
}
