package org.springframework.samples.petclinic.test.component.customers;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * customers-service as a real process in a container, called directly over HTTP.
 * The database is checked through the DAOs, not through the service itself.
 */
@Epic("Customers service")
@Feature("Owners and pets")
@Tag("component")
class CustomersServiceComponentTest extends BaseComponentTest {

    private final List<Integer> createdOwnerIds = new ArrayList<>();

    @Override
    protected String defaultJarPath() {
        return "../../app/spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar";
    }

    @Override
    protected String readinessPath() {
        return "/petTypes";
    }

    @AfterEach
    void deleteCreatedOwners() {
        for (int id : createdOwnerIds) {
            database.pets().deleteByOwnerId(id);
            database.owners().deleteById(id);
        }
        createdOwnerIds.clear();
    }

    @Test
    @DisplayName("GET /owners returns the owners of the database, with the seeded George Franklin as owner 1")
    void ownersMatchDatabase() {
        List<Integer> idsInDatabase = database.owners().findAll().stream().map(OwnerDto::id).toList();
        assertThat(idsInDatabase).contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> idsFromService = given(service).get("/owners")
            .then().statusCode(200)
            .body("find { it.id == 1 }.firstName", equalTo("George"))
            .body("find { it.id == 1 }.lastName", equalTo("Franklin"))
            .extract().path("id");

        assertThat(idsFromService).containsExactlyInAnyOrderElementsOf(idsInDatabase);
    }

    @Test
    @DisplayName("POST /owners creates the owner and stores it in the database")
    void createOwnerStoresRow() {
        OwnerRequest request = TestDataGenerator.generateOwnerRequest();

        int id = given(service).body(request).post("/owners")
            .then().statusCode(201)
            .body("firstName", equalTo(request.firstName()))
            .body("telephone", equalTo(request.telephone()))
            .extract().path("id");
        createdOwnerIds.add(id);

        OwnerDto row = database.owners().findById(id).orElseThrow();
        assertThat(row.firstName()).isEqualTo(request.firstName());
        assertThat(row.lastName()).isEqualTo(request.lastName());
        assertThat(row.address()).isEqualTo(request.address());
        assertThat(row.city()).isEqualTo(request.city());
        assertThat(row.telephone()).isEqualTo(request.telephone());
    }

    @Test
    @DisplayName("GET /owners/{id} returns the owner that is in the database")
    void getOwnerReturnsRow() {
        int id = database.owners().insert(TestDataGenerator.generateOwnerRequest("Seeded"));
        createdOwnerIds.add(id);
        OwnerDto row = database.owners().findById(id).orElseThrow();

        given(service).get("/owners/{id}", id)
            .then().statusCode(200)
            .body("id", equalTo(id))
            .body("lastName", equalTo(row.lastName()))
            .body("telephone", equalTo(row.telephone()));
    }

    @Test
    @DisplayName("PUT /owners/{id} updates the row")
    void updateOwnerChangesRow() {
        int id = database.owners().insert(TestDataGenerator.generateOwnerRequest());
        createdOwnerIds.add(id);
        OwnerRequest update = TestDataGenerator.generateOwnerRequest();

        given(service).body(update).put("/owners/{id}", id).then().statusCode(204);

        OwnerDto row = database.owners().findById(id).orElseThrow();
        assertThat(row.firstName()).isEqualTo(update.firstName());
        assertThat(row.telephone()).isEqualTo(update.telephone());
    }

    @Tag("negative")
    @Test
    @DisplayName("POST /owners with an empty body is rejected with 400 and stores nothing")
    void emptyBodyIsRejected() {
        long before = database.owners().count();

        given(service).body("{}").post("/owners").then().statusCode(400);

        assertThat(database.owners().count()).isEqualTo(before);
    }

    @Tag("negative")
    @ParameterizedTest(name = "telephone \"{0}\" is rejected with 400")
    @ValueSource(strings = {"abc", "12345678901234", "12-34"})
    void invalidTelephoneIsRejected(String telephone) {
        OwnerRequest valid = TestDataGenerator.generateOwnerRequest();
        OwnerRequest invalid = new OwnerRequest(valid.firstName(), valid.lastName(), valid.address(), valid.city(), telephone);

        given(service).body(invalid).post("/owners").then().statusCode(400);

        assertThat(database.owners().findByTelephone(telephone)).isEmpty();
    }

    @Tag("negative")
    @ParameterizedTest(name = "blank firstName \"{0}\" is rejected with 400")
    @ValueSource(strings = {"", "   "})
    void blankFirstNameIsRejected(String firstName) {
        OwnerRequest valid = TestDataGenerator.generateOwnerRequest();
        OwnerRequest invalid = new OwnerRequest(firstName, valid.lastName(), valid.address(), valid.city(), valid.telephone());

        given(service).body(invalid).post("/owners").then().statusCode(400);

        assertThat(database.owners().findByTelephone(valid.telephone())).isEmpty();
    }

    @Tag("negative")
    @ParameterizedTest(name = "GET /owners/{0} is rejected with 400")
    @ValueSource(ints = {0, -1})
    void nonPositiveIdIsRejected(int id) {
        given(service).get("/owners/{id}", id).then().statusCode(400);
    }

    @Tag("negative")
    @Test
    @DisplayName("GET /owners/{id} for a missing owner returns 404")
    void missingOwnerIsNotFound() {
        given(service).get("/owners/{id}", 99999).then().statusCode(404);
    }

    private int newOwnerId() {
        int id = database.owners().insert(TestDataGenerator.generateOwnerRequest());
        createdOwnerIds.add(id);
        return id;
    }

    // ---- pet types ----

    @Test
    @DisplayName("GET /petTypes returns the six seeded types, as in the database")
    void petTypesMatchDatabase() {
        List<String> expected = database.pets().findAllTypes().stream().map(t -> t.name()).toList();

        assertThat(expected).hasSize(6);
        given(service).get("/petTypes")
            .then().statusCode(200)
            .body("", hasSize(6))
            .body("name", org.hamcrest.Matchers.containsInAnyOrder(expected.toArray()));
    }

    // ---- pets ----

    @Test
    @DisplayName("GET /owners/{ownerId}/pets/{petId} returns a seeded pet as stored in the database")
    void getSeededPet() {
        PetDto row = database.pets().findById(1).orElseThrow();

        given(service).get("/owners/1/pets/1")
            .then().statusCode(200)
            .body("id", equalTo(1))
            .body("name", equalTo(row.name()))
            .body("type.name", equalTo(row.type().name()));
    }

    @Test
    @DisplayName("POST /owners/{id}/pets creates the pet and stores it for that owner")
    void createPetStoresRow() {
        int ownerId = newOwnerId();
        PetRequest request = TestDataGenerator.generatePetRequest(2);

        int petId = given(service).body(request).post("/owners/{id}/pets", ownerId)
            .then().statusCode(201)
            .body("name", equalTo(request.name()))
            .body("type.id", equalTo(2))
            .extract().path("id");

        PetDto row = database.pets().findById(petId).orElseThrow();
        assertThat(row.name()).isEqualTo(request.name());
        assertThat(row.birthDate()).isEqualTo(LocalDate.parse(request.birthDate()));
        assertThat(row.type().id()).isEqualTo(2);
        assertThat(database.pets().findByOwnerId(ownerId)).extracting(PetDto::id).containsExactly(petId);
    }

    @Test
    @DisplayName("PUT /owners/{id}/pets/{petId} updates name, birth date and type")
    void updatePetChangesRow() {
        int ownerId = newOwnerId();
        int petId = database.pets().insertFields("Rex", "2020-01-02", 2, ownerId);
        PetRequest update = new PetRequest("Tom", "2019-03-04", 1);

        given(service).body(update).put("/owners/{id}/pets/{petId}", ownerId, petId)
            .then().statusCode(204);

        PetDto row = database.pets().findById(petId).orElseThrow();
        assertThat(row.name()).isEqualTo("Tom");
        assertThat(row.birthDate()).isEqualTo(LocalDate.of(2019, 3, 4));
        assertThat(row.type().id()).isEqualTo(1);
    }

    @Tag("negative")
    @Test
    @DisplayName("POST pet for a missing owner returns 404 and stores nothing")
    void petForMissingOwnerIsNotFound() {
        PetRequest request = TestDataGenerator.generatePetRequest(1);

        given(service).body(request).post("/owners/{id}/pets", 99999).then().statusCode(404);

        assertThat(database.pets().findByOwnerId(99999)).isEmpty();
    }

    @Tag("negative")
    @Test
    @DisplayName("GET a missing pet returns 404")
    void missingPetIsNotFound() {
        given(service).get("/owners/1/pets/{petId}", 99999).then().statusCode(404);
    }

    @Tag("negative")
    @Test
    @DisplayName("PUT a missing pet returns 404")
    void updateMissingPetIsNotFound() {
        given(service).body(TestDataGenerator.generatePetRequest(1))
            .put("/owners/1/pets/{petId}", 99999).then().statusCode(404);
    }

    @Tag("negative")
    @Test
    @DisplayName("A pet with an empty name is rejected with 400")
    void emptyPetNameIsRejected() {
        int ownerId = newOwnerId();

        given(service).body(new PetRequest("", "2020-01-02", 1)).post("/owners/{id}/pets", ownerId)
            .then().statusCode(400);

        assertThat(database.pets().findByOwnerId(ownerId)).isEmpty();
    }

    @Tag("negative")
    @Test
    @DisplayName("A pet born in the future is rejected with 400")
    void futureBirthDateIsRejected() {
        int ownerId = newOwnerId();
        String tomorrow = LocalDate.now().plusDays(2).toString();

        given(service).body(new PetRequest("Future", tomorrow, 1)).post("/owners/{id}/pets", ownerId)
            .then().statusCode(400);

        assertThat(database.pets().findByOwnerId(ownerId)).isEmpty();
    }

    // ---- known application defects: these tests fail on purpose until the application is fixed ----

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #5: a pet with an unknown type is rejected with 400 (the service answers 500)")
    void unknownPetTypeIsRejected() {
        int ownerId = newOwnerId();

        given(service).body(new PetRequest("Ghost", "2020-01-02", 99)).post("/owners/{id}/pets", ownerId)
            .then().statusCode(400);

        assertThat(database.pets().findByOwnerId(ownerId)).isEmpty();
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #3: the pet birth date is returned as yyyy-MM-dd, like visit dates (the service returns a UTC date-time)")
    void petBirthDateFormatIsPlainDate() {
        int ownerId = newOwnerId();
        int petId = database.pets().insertFields("Dated", "2000-09-07", 1, ownerId);

        given(service).get("/owners/{id}/pets/{petId}", ownerId, petId)
            .then().statusCode(200)
            .body("birthDate", matchesPattern("\\d{4}-\\d{2}-\\d{2}"));
    }
}
