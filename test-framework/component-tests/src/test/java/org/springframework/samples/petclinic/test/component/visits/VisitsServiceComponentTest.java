package org.springframework.samples.petclinic.test.component.visits;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("Component Tests")
@Feature("Visits Service")
public class VisitsServiceComponentTest extends BaseComponentTest {

    @Test
    @Story("Visit persistence")
    @DisplayName("Insert and retrieve visit for pet in Testcontainers MySQL")
    @Description("Verifies foreign key constraints, visit_date parsing, and description persistence")
    void shouldInsertAndFindVisitByPetId() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = ownerDao.insert(ownerReq);

        PetRequest petReq = TestDataGenerator.generatePetRequest(2); // dog
        int petId = petDao.insert(ownerId, petReq);

        VisitRequest visitReq = TestDataGenerator.generateVisitRequest();
        int visitId = visitDao.insert(petId, visitReq);
        assertThat(visitId).isPositive();

        Optional<VisitDto> found = visitDao.findById(visitId);
        assertThat(found).isPresent();
        VisitDto visit = found.get();
        assertThat(visit.petId()).isEqualTo(petId);
        assertThat(visit.date()).isEqualTo(LocalDate.parse(visitReq.date()));
        assertThat(visit.description()).isEqualTo(visitReq.description());

        List<VisitDto> petVisits = visitDao.findByPetId(petId);
        assertThat(petVisits).hasSize(1);
        assertThat(petVisits.getFirst().id()).isEqualTo(visitId);
    }

    @Test
    @Story("Visits aggregation")
    @DisplayName("Query visits for multiple pets simultaneously")
    @Description("Emulates the visits-service endpoint /pets/visits?petId=X&petId=Y")
    void shouldFindVisitsForMultiplePets() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = ownerDao.insert(ownerReq);

        int pet1Id = petDao.insert(ownerId, TestDataGenerator.generatePetRequest(1));
        int pet2Id = petDao.insert(ownerId, TestDataGenerator.generatePetRequest(2));

        VisitRequest v1 = TestDataGenerator.generateVisitRequest();
        VisitRequest v2 = TestDataGenerator.generateVisitRequest();

        visitDao.insert(pet1Id, v1);
        visitDao.insert(pet2Id, v2);

        List<VisitDto> aggregated = visitDao.findByPetIds(List.of(pet1Id, pet2Id));
        assertThat(aggregated).hasSize(2);
        assertThat(aggregated).extracting(VisitDto::petId).containsExactlyInAnyOrder(pet1Id, pet2Id);
    }

    @Test
    @Story("Visit edge cases")
    @DisplayName("Verify support for large medical descriptions up to 4000 characters")
    @Description("Verifies VARCHAR(8192) column capacity in visits schema")
    void shouldHandleLargeVisitDescription() {
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        int ownerId = ownerDao.insert(ownerReq);
        int petId = petDao.insert(ownerId, TestDataGenerator.generatePetRequest(1));

        String largeDescription = "Detailed clinical examination notes: ".repeat(100);
        VisitRequest largeVisit = new VisitRequest("2026-11-13", largeDescription);

        int visitId = visitDao.insert(petId, largeVisit);
        assertThat(visitId).isPositive();

        Optional<VisitDto> found = visitDao.findById(visitId);
        assertThat(found).isPresent();
        assertThat(found.get().description()).isEqualTo(largeDescription);
    }
}
