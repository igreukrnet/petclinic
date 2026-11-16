package org.springframework.samples.petclinic.test.component.vets;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

/**
 * vets-service as a real process in a container, called directly over HTTP.
 * The expected data comes from the database through the DAOs.
 */
@Epic("Vets service")
@Feature("Veterinarians")
@Tag("component")
class VetsServiceComponentTest extends BaseComponentTest {

    @Override
    protected String defaultJarPath() {
        return "../../app/spring-petclinic-vets-service/target/spring-petclinic-vets-service-4.0.1.jar";
    }

    @Override
    protected String readinessPath() {
        return "/vets";
    }

    @Test
    @DisplayName("GET /vets returns the six seeded vets with their specialties, as in the database")
    void vetsMatchDatabase() {
        List<VetDto> expected = database.vets().findAllWithSpecialties();
        assertThat(expected).hasSize(6);

        VetDto[] actual = given(service).get("/vets")
            .then().statusCode(200)
            .extract().as(VetDto[].class);

        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("A vet with two specialties lists both, sorted by name; a vet without specialties has an empty list")
    void specialtiesAreListed() {
        given(service).get("/vets")
            .then().statusCode(200)
            .body("find { it.lastName == 'Douglas' }.specialties.name", contains("dentistry", "surgery"))
            .body("find { it.lastName == 'Carter' }.specialties", empty())
            .body("", hasSize(6));
    }

    @Tag("negative")
    @Test
    @DisplayName("POST /vets is not allowed: the resource is read-only")
    void vetsCannotBeCreated() {
        long before = database.vets().findAllVets().size();

        given(service).body("{\"firstName\":\"New\",\"lastName\":\"Vet\"}").post("/vets")
            .then().statusCode(405);

        assertThat(database.vets().findAllVets()).hasSize((int) before);
    }
}
