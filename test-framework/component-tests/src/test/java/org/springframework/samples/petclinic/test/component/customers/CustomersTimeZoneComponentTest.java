package org.springframework.samples.petclinic.test.component.customers;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.data.TestDataGenerator;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * customers-service in a JVM with a non-UTC time zone. The E2E stack runs with {@code TZ=UTC}, so this
 * dependency on the server time zone is invisible there.
 */
@Epic("Customers service")
@Feature("Time zone")
@Tag("component")
class CustomersTimeZoneComponentTest extends BaseComponentTest {

    @Override
    protected String defaultJarPath() {
        return "../../app/spring-petclinic-customers-service/target/spring-petclinic-customers-service-4.0.1.jar";
    }

    @Override
    protected String readinessPath() {
        return "/petTypes";
    }

    @Override
    protected String timeZone() {
        return "Europe/Kyiv";
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #3: the pet birth date does not depend on the server time zone (it shifts one day back in Europe/Kyiv)")
    void petBirthDateDoesNotDependOnServerTimeZone() {
        int ownerId = database.owners().insert(TestDataGenerator.generateOwnerRequest());
        try {
            int petId = database.pets().insertFields("Zoned", "2000-09-07", 1, ownerId);

            String body = given(service).get("/owners/{id}/pets/{petId}", ownerId, petId)
                .then().statusCode(200).extract().path("birthDate");

            assertThat(toLocalDate(body)).isEqualTo(LocalDate.of(2000, 9, 7));
        } finally {
            database.pets().deleteByOwnerId(ownerId);
            database.owners().deleteById(ownerId);
        }
    }

    /** A plain date is read as is; a date-time is read in UTC, as any client of the API would. */
    private static LocalDate toLocalDate(String value) {
        if (value.length() == 10) {
            return LocalDate.parse(value);
        }
        return OffsetDateTime.parse(value).toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
    }
}
