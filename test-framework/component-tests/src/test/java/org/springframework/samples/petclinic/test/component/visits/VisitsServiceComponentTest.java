package org.springframework.samples.petclinic.test.component.visits;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * visits-service as a real process in a container, called directly over HTTP.
 */
@Epic("Visits service")
@Feature("Visits")
@Tag("component")
class VisitsServiceComponentTest extends BaseVisitsTest {

    /** Seed: pet 7 has visits 1 and 4, pet 8 has visits 2 and 3, pet 1 has none. */
    private static final int PET_WITH_TWO_VISITS = 7;
    private static final int OTHER_PET_WITH_VISITS = 8;
    private static final int PET_WITHOUT_VISITS = 1;

    private final List<Integer> createdVisitIds = new ArrayList<>();

    @AfterEach
    void deleteCreatedVisits() {
        createdVisitIds.forEach(database.visits()::deleteById);
        createdVisitIds.clear();
    }

    @Test
    @DisplayName("GET /owners/{o}/pets/{p}/visits returns the visits of the pet, as in the database")
    void visitsOfPetMatchDatabase() {
        List<VisitDto> expected = database.visits().findByPetId(PET_WITH_TWO_VISITS);
        assertThat(expected).hasSize(2);

        VisitDto[] actual = given(service).get("/owners/6/pets/{petId}/visits", PET_WITH_TWO_VISITS)
            .then().statusCode(200)
            .extract().as(VisitDto[].class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("A pet without visits gets an empty list")
    void petWithoutVisitsGetsEmptyList() {
        assertThat(database.visits().findByPetId(PET_WITHOUT_VISITS)).isEmpty();

        given(service).get("/owners/1/pets/{petId}/visits", PET_WITHOUT_VISITS)
            .then().statusCode(200)
            .body("", empty());
    }

    @Test
    @DisplayName("POST creates the visit for the pet in the path and stores it in the database")
    void createVisitStoresRow() {
        int id = given(service).body(Map.of("date", "2024-05-06", "description", "annual checkup"))
            .post("/owners/1/pets/{petId}/visits", PET_WITHOUT_VISITS)
            .then().statusCode(201)
            .body("petId", equalTo(PET_WITHOUT_VISITS))
            .body("date", equalTo("2024-05-06"))
            .body("description", equalTo("annual checkup"))
            .extract().path("id");
        createdVisitIds.add(id);

        VisitDto row = database.visits().findById(id).orElseThrow();
        assertThat(row.petId()).isEqualTo(PET_WITHOUT_VISITS);
        assertThat(row.date().toString()).isEqualTo("2024-05-06");
        assertThat(row.description()).isEqualTo("annual checkup");
    }

    @Test
    @DisplayName("The pet in the path wins over a petId in the body")
    void petIdInPathWinsOverBody() {
        int id = given(service)
            .body(Map.of("date", "2024-05-06", "description", "x", "petId", OTHER_PET_WITH_VISITS))
            .post("/owners/1/pets/{petId}/visits", PET_WITHOUT_VISITS)
            .then().statusCode(201)
            .extract().path("id");
        createdVisitIds.add(id);

        assertThat(database.visits().findById(id).orElseThrow().petId()).isEqualTo(PET_WITHOUT_VISITS);
    }

    @Test
    @DisplayName("GET /pets/visits?petId=7,8 returns the visits of both pets, as in the database")
    void visitsOfSeveralPets() {
        List<VisitDto> expected = database.visits().findByPetIds(List.of(PET_WITH_TWO_VISITS, OTHER_PET_WITH_VISITS));
        assertThat(expected).hasSize(4);

        given(service).get("/pets/visits?petId=7,8")
            .then().statusCode(200)
            .body("items", hasSize(4))
            .body("items.id", containsInAnyOrder(expected.stream().map(VisitDto::id).toArray()));
    }

    @Test
    @DisplayName("GET /pets/visits with an unknown pet returns an empty list, not an error")
    void unknownPetInBulkQueryGivesEmptyItems() {
        given(service).get("/pets/visits?petId=99999")
            .then().statusCode(200)
            .body("items", empty());
    }

    @Tag("negative")
    @Test
    @DisplayName("GET /pets/visits without petId is rejected with 400")
    void bulkQueryRequiresPetId() {
        given(service).get("/pets/visits").then().statusCode(400);
    }

    @Tag("negative")
    @Test
    @DisplayName("A pet id below 1 is rejected with 400, on GET and on POST")
    void nonPositivePetIdIsRejected() {
        given(service).get("/owners/1/pets/0/visits").then().statusCode(400);

        long before = database.visits().findByPetIds(List.of(1, 7, 8)).size();
        given(service).body(Map.of("date", "2024-05-06", "description", "x"))
            .post("/owners/1/pets/0/visits").then().statusCode(400);
        assertThat(database.visits().findByPetIds(List.of(1, 7, 8))).hasSize((int) before);
    }

    @Tag("negative")
    @Test
    @DisplayName("A description longer than 8192 characters is rejected with 400 and stores nothing")
    void tooLongDescriptionIsRejected() {
        given(service).body(Map.of("date", "2024-05-06", "description", "x".repeat(8193)))
            .post("/owners/1/pets/{petId}/visits", PET_WITHOUT_VISITS)
            .then().statusCode(400);

        assertThat(database.visits().findByPetId(PET_WITHOUT_VISITS)).isEmpty();
    }

    @Tag("negative")
    @Test
    @DisplayName("A date that is not yyyy-MM-dd is rejected with 400 and stores nothing")
    void malformedDateIsRejected() {
        given(service).body(Map.of("date", "05/06/2024", "description", "x"))
            .post("/owners/1/pets/{petId}/visits", PET_WITHOUT_VISITS)
            .then().statusCode(400);

        assertThat(database.visits().findByPetId(PET_WITHOUT_VISITS)).isEmpty();
    }

    // ---- known application defects: these tests fail on purpose until the application is fixed ----

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #4: a visit for a missing pet is rejected with 404 (the service answers 500)")
    void visitForMissingPetIsNotFound() {
        given(service).body(Map.of("date", "2024-05-06", "description", "x"))
            .post("/owners/1/pets/99999/visits")
            .then().statusCode(404);

        assertThat(database.visits().findByPetId(99999)).isEmpty();
    }
}
