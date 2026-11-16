package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

@Epic("E2E Tests")
@Feature("REST API & Gateway Aggregation")
public class PetClinicApiE2ETest extends BaseE2ETest {

    @Test
    @Story("Gateway routed API and database consistency")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Execute full owner, pet, visit lifecycle via Gateway REST endpoints and verify in MySQL")
    @Description("Tests StripPrefix routing, ApiGatewayController async aggregation and database state")
    void shouldCreateOwnerPetVisitViaApiAndVerifyInGatewayAndDatabase() {
        // Step 1: Create owner via Gateway
        OwnerRequest ownerReq = TestDataGenerator.generateOwnerRequest();
        Response ownerResponse = petClinicClient.createOwner(ownerReq);
        ownerResponse.then().statusCode(201);
        int ownerId = ownerResponse.jsonPath().getInt("id");
        assertThat(ownerId).isPositive();
        trackOwner(ownerId);

        // Step 2: Add pet to owner
        PetRequest petReq = TestDataGenerator.generatePetRequest(2); // dog
        Response petResponse = petClinicClient.addPet(ownerId, petReq);
        petResponse.then().statusCode(201);
        int petId = petResponse.jsonPath().getInt("id");
        assertThat(petId).isPositive();

        // Step 3: Add visit for pet
        VisitRequest visitReq = TestDataGenerator.generateVisitRequest();
        Response visitResponse = petClinicClient.addVisit(ownerId, petId, visitReq);
        visitResponse.then().statusCode(201);
        int visitId = visitResponse.jsonPath().getInt("id");
        assertThat(visitId).isPositive();

        // Step 4: Verify aggregated details from Gateway
        Response aggregatedResponse = petClinicClient.getAggregatedOwnerDetails(ownerId);
        aggregatedResponse.then()
            .statusCode(200)
            .body("id", equalTo(ownerId))
            .body("firstName", equalTo(ownerReq.firstName()))
            .body("lastName", equalTo(ownerReq.lastName()))
            .body("pets", hasSize(1))
            .body("pets[0].id", equalTo(petId))
            .body("pets[0].name", equalTo(petReq.name()))
            .body("pets[0].visits", hasSize(1))
            .body("pets[0].visits[0].description", equalTo(visitReq.description()));

        // Step 4b: Dates in the aggregate are checked as dates, not strings.
        // customers-service sends pet birth dates as UTC date-times, visits-service as yyyy-MM-dd.
        OwnerDto aggregatedOwner = aggregatedResponse.as(OwnerDto.class);
        PetDto aggregatedPet = aggregatedOwner.pets().get(0);
        assertThat(aggregatedPet.birthDate()).isEqualTo(LocalDate.parse(petReq.birthDate()));
        assertThat(aggregatedPet.visits().get(0).date()).isEqualTo(LocalDate.parse(visitReq.date()));

        // Step 5: SQL Direct Verification via JDBI
        Optional<OwnerDto> dbOwner = ownerDao.findById(ownerId);
        assertThat(dbOwner).isPresent();
        assertThat(dbOwner.get().telephone()).isEqualTo(ownerReq.telephone());

        Optional<PetDto> dbPet = petDao.findById(petId);
        assertThat(dbPet).isPresent();
        assertThat(dbPet.get().name()).isEqualTo(petReq.name());

        Optional<VisitDto> dbVisit = visitDao.findById(visitId);
        assertThat(dbVisit).isPresent();
        assertThat(dbVisit.get().description()).isEqualTo(visitReq.description());

        // Step 6: Verify Vets endpoint and SQL match
        Response vetsResponse = petClinicClient.getVets();
        vetsResponse.then().statusCode(200);
        List<VetDto> apiVets = vetsResponse.jsonPath().getList(".", VetDto.class);
        List<VetDto> dbVets = vetDao.findAllWithSpecialties();
        assertThat(apiVets.size()).isEqualTo(dbVets.size());
    }
}
