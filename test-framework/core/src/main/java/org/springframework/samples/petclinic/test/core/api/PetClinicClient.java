package org.springframework.samples.petclinic.test.core.api;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.util.List;

import static io.restassured.RestAssured.given;

public class PetClinicClient {

    private final ApiClient apiClient;

    public PetClinicClient() {
        this.apiClient = new ApiClient();
    }

    public PetClinicClient(String baseUrl) {
        this.apiClient = new ApiClient(baseUrl);
    }

    public PetClinicClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    private RequestSpecification req() {
        return given().spec(apiClient.getSpec());
    }

    @Step("API: Create owner: {request}")
    public Response createOwner(OwnerRequest request) {
        return req()
            .body(request)
            .when()
            .post("/api/customer/owners");
    }

    @Step("API: Create owner and return OwnerDto")
    public OwnerDto createOwnerAndGet(OwnerRequest request) {
        return createOwner(request)
            .then()
            .statusCode(201)
            .extract()
            .as(OwnerDto.class);
    }

    @Step("API: Get owner by id: {ownerId}")
    public Response getOwner(int ownerId) {
        return req()
            .when()
            .get("/api/customer/owners/{ownerId}", ownerId);
    }

    @Step("API: Get all owners")
    public Response getAllOwners() {
        return req()
            .when()
            .get("/api/customer/owners");
    }

    @Step("API: Update owner id: {ownerId}")
    public Response updateOwner(int ownerId, OwnerRequest request) {
        return req()
            .body(request)
            .when()
            .put("/api/customer/owners/{ownerId}", ownerId);
    }

    @Step("API: Get pet types")
    public Response getPetTypes() {
        return req()
            .when()
            .get("/api/customer/petTypes");
    }

    @Step("API: Add pet to owner {ownerId}: {request}")
    public Response addPet(int ownerId, PetRequest request) {
        return req()
            .body(request)
            .when()
            .post("/api/customer/owners/{ownerId}/pets", ownerId);
    }

    @Step("API: Add visit to pet {petId} (owner {ownerId}): {request}")
    public Response addVisit(int ownerId, int petId, VisitRequest request) {
        return req()
            .body(request)
            .when()
            .post("/api/visit/owners/{ownerId}/pets/{petId}/visits", ownerId, petId);
    }

    @Step("API: Get visits for owner {ownerId} and pet {petId}")
    public Response getVisits(int ownerId, int petId) {
        return req()
            .when()
            .get("/api/visit/owners/{ownerId}/pets/{petId}/visits", ownerId, petId);
    }

    @Step("API: Get visits for multiple petIds: {petIds}")
    public Response getVisitsForPets(List<Integer> petIds) {
        var spec = req();
        for (Integer id : petIds) {
            spec = spec.queryParam("petId", id);
        }
        return spec.when().get("/api/visit/pets/visits");
    }

    @Step("API Gateway: Get aggregated owner details by id: {ownerId}")
    public Response getAggregatedOwnerDetails(int ownerId) {
        return req()
            .when()
            .get("/api/gateway/owners/{ownerId}", ownerId);
    }

    @Step("API: Get all vets")
    public Response getVets() {
        return req()
            .when()
            .get("/api/vet/vets");
    }
}
