package org.springframework.samples.petclinic.test.component.visits;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * visits-service in a JVM with a non-UTC time zone. The E2E stack runs with {@code TZ=UTC}, so a dependency
 * of visit dates on the server time zone is invisible there.
 */
@Epic("Visits service")
@Feature("Time zone")
@Tag("component")
class VisitsTimeZoneComponentTest extends BaseVisitsTest {

    @Override
    protected String timeZone() {
        return "Europe/Kyiv";
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #14: the date of a stored visit does not depend on the server time zone (it shifts one day back in Europe/Kyiv)")
    void visitDateDoesNotDependOnServerTimeZone() {
        // Seed visit 1: pet 7, 2010-03-04.
        given(service).get("/owners/6/pets/7/visits")
            .then().statusCode(200)
            .body("find { it.id == 1 }.date", equalTo("2010-03-04"));
    }
}
