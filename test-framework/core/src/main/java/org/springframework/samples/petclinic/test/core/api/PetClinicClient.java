package org.springframework.samples.petclinic.test.core.api;

import io.qameta.allure.Step;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * REST client for the gateway.
 * <p>
 * Methods that return {@link Response} do not check the status code: negative tests need the raw answer,
 * and the test asserts the status itself.
 * <p>
 * Methods whose names end with {@code Dto} or that return DTOs read the body into the DTO. They throw
 * {@link IllegalStateException} with the status and body for any non-2xx answer, so an error body is
 * never silently mapped to an object with empty fields.
 */
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

    // ---- Owners ---------------------------------------------------------------------------------

    @Step("API: Create owner: {request}")
    public Response createOwner(OwnerRequest request) {
        return req()
            .body(request)
            .when()
            .post("/api/customer/owners");
    }

    @Step("API: Get owner by id: {ownerId}")
    public Response getOwner(int ownerId) {
        return req()
            .when()
            .get("/api/customer/owners/{ownerId}", ownerId);
    }

    @Step("API: Get owner DTO by id: {ownerId}")
    public OwnerDto getOwnerDto(int ownerId) {
        return requireSuccess(getOwner(ownerId), "Get owner " + ownerId).as(OwnerDto.class);
    }

    @Step("API: Get all owners")
    public Response getAllOwners() {
        return req()
            .when()
            .get("/api/customer/owners");
    }

    @Step("API: Get all owner DTOs")
    public List<OwnerDto> getAllOwnerDtos() {
        return requireSuccess(getAllOwners(), "Get all owners").as(new TypeRef<List<OwnerDto>>() {});
    }

    @Step("API: Update owner id: {ownerId}")
    public Response updateOwner(int ownerId, OwnerRequest request) {
        return req()
            .body(request)
            .when()
            .put("/api/customer/owners/{ownerId}", ownerId);
    }

    @Step("API Gateway: Get aggregated owner details by id: {ownerId}")
    public Response getAggregatedOwnerDetails(int ownerId) {
        return req()
            .when()
            .get("/api/gateway/owners/{ownerId}", ownerId);
    }

    @Step("API Gateway: Get aggregated owner DTO by id: {ownerId}")
    public OwnerDto getAggregatedOwnerDto(int ownerId) {
        return requireSuccess(getAggregatedOwnerDetails(ownerId), "Get aggregated owner " + ownerId)
            .as(OwnerDto.class);
    }

    // ---- Pets -----------------------------------------------------------------------------------

    @Step("API: Add pet to owner {ownerId}: {request}")
    public Response addPet(int ownerId, PetRequest request) {
        return req()
            .body(request)
            .when()
            .post("/api/customer/owners/{ownerId}/pets", ownerId);
    }

    @Step("API: Get pet {petId} of owner {ownerId}")
    public Response getPet(int ownerId, int petId) {
        return req()
            .when()
            .get("/api/customer/owners/{ownerId}/pets/{petId}", ownerId, petId);
    }

    @Step("API: Get pet DTO {petId} of owner {ownerId}")
    public PetDto getPetDto(int ownerId, int petId) {
        return requireSuccess(getPet(ownerId, petId), "Get pet " + petId).as(PetDto.class);
    }

    @Step("API: Update pet {petId} of owner {ownerId}: {request}")
    public Response updatePet(int ownerId, int petId, PetRequest request) {
        return req()
            .body(request)
            .when()
            .put("/api/customer/owners/{ownerId}/pets/{petId}", ownerId, petId);
    }

    @Step("API: Get pet types")
    public Response getPetTypes() {
        return req()
            .when()
            .get("/api/customer/petTypes");
    }

    @Step("API: Get pet type DTOs")
    public List<PetTypeDto> getPetTypeDtos() {
        return requireSuccess(getPetTypes(), "Get pet types").as(new TypeRef<List<PetTypeDto>>() {});
    }

    // ---- Visits ---------------------------------------------------------------------------------

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

    @Step("API: Get visit DTOs for owner {ownerId} and pet {petId}")
    public List<VisitDto> getVisitDtos(int ownerId, int petId) {
        return requireSuccess(getVisits(ownerId, petId), "Get visits of pet " + petId)
            .as(new TypeRef<List<VisitDto>>() {});
    }

    @Step("API: Get visits for multiple petIds: {petIds}")
    public Response getVisitsForPets(List<Integer> petIds) {
        var spec = req();
        for (Integer id : petIds) {
            spec = spec.queryParam("petId", id);
        }
        return spec.when().get("/api/visit/pets/visits");
    }

    // ---- Vets -----------------------------------------------------------------------------------

    @Step("API: Get all vets")
    public Response getVets() {
        return req()
            .when()
            .get("/api/vet/vets");
    }

    @Step("API: Get all vet DTOs")
    public List<VetDto> getVetDtos() {
        return requireSuccess(getVets(), "Get vets").as(new TypeRef<List<VetDto>>() {});
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private static Response requireSuccess(Response response, String operation) {
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(operation + " failed with HTTP " + status + ": " + response.asString());
        }
        return response;
    }
}
