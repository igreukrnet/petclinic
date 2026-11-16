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
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * customers-service as a real process in a container, called directly over HTTP.
 * The database is checked through the DAOs, not through the service itself.
 */
@Epic("Customers service")
@Feature("Owners")
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
    @DisplayName("Seed data: 10 owners, George Franklin is owner 1")
    void seedDataIsLoaded() {
        given(service).get("/owners")
            .then().statusCode(200)
            .body("", hasSize(10))
            .body("find { it.id == 1 }.firstName", equalTo("George"))
            .body("find { it.id == 1 }.lastName", equalTo("Franklin"));
        assertThat(database.owners().count()).isEqualTo(10);
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
}
