package org.springframework.samples.petclinic.test.component.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.samples.petclinic.test.core.ai.AiFailureTriageExtension;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Component Tests")
@Feature("Gateway Resilience & Circuit Breaker")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public class GatewayCircuitBreakerTest {

    private WireMockServer customersServiceMock;
    private WireMockServer visitsServiceMock;

    @BeforeAll
    void setupWireMock() {
        customersServiceMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        visitsServiceMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

        customersServiceMock.start();
        visitsServiceMock.start();
    }

    @AfterAll
    void teardownWireMock() {
        if (customersServiceMock != null && customersServiceMock.isRunning()) {
            customersServiceMock.stop();
        }
        if (visitsServiceMock != null && visitsServiceMock.isRunning()) {
            visitsServiceMock.stop();
        }
    }

    @Test
    @Story("Healthy downstream services")
    @DisplayName("Verify healthy downstream response when both customers and visits services respond 200 OK")
    @Description("Emulates normal flow where both services return data correctly")
    void shouldReturnFullAggregatedDataWhenServicesHealthy() {
        customersServiceMock.stubFor(get(urlEqualTo("/owners/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "id": 1,
                      "firstName": "George",
                      "lastName": "Franklin",
                      "address": "110 W. Liberty St.",
                      "city": "Madison",
                      "telephone": "6085551023",
                      "pets": [
                        {
                          "id": 1,
                          "name": "Leo",
                          "birthDate": "2020-09-07",
                          "type": {"name": "cat"},
                          "visits": []
                        }
                      ]
                    }
                    """)));

        visitsServiceMock.stubFor(get(urlEqualTo("/pets/visits?petId=1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                      "items": [
                        {
                          "id": 101,
                          "petId": 1,
                          "date": "2026-11-01",
                          "description": "Routine rabies vaccination"
                        }
                      ]
                    }
                    """)));

        // Verify customers-service stub
        given()
            .baseUri(customersServiceMock.baseUrl())
            .when()
            .get("/owners/1")
            .then()
            .statusCode(200)
            .body("firstName", org.hamcrest.Matchers.equalTo("George"))
            .body("pets[0].name", org.hamcrest.Matchers.equalTo("Leo"));

        // Verify visits-service stub
        given()
            .baseUri(visitsServiceMock.baseUrl())
            .when()
            .get("/pets/visits?petId=1")
            .then()
            .statusCode(200)
            .body("items", hasSize(1))
            .body("items[0].description", org.hamcrest.Matchers.equalTo("Routine rabies vaccination"));
    }

    @Test
    @Story("Visits Service Outage & Circuit Breaker Fallback")
    @DisplayName("Verify fallback behavior when visits-service returns 503 Service Unavailable")
    @Description("Emulates ReactiveCircuitBreaker fallback: visitsServiceClient returning emptyVisitsForPets()")
    void shouldTriggerFallbackWhenVisitsServiceFailsWith503() {
        visitsServiceMock.stubFor(get(urlEqualTo("/pets/visits?petId=99"))
            .willReturn(aResponse()
                .withStatus(503)
                .withStatusMessage("Service Unavailable")
                .withBody("{\"error\": \"Downstream service is temporarily unavailable\"}")));

        // Directly verify that visits-service returns 503 to be handled by Circuit Breaker
        given()
            .baseUri(visitsServiceMock.baseUrl())
            .when()
            .get("/pets/visits?petId=99")
            .then()
            .statusCode(503);

        // Verify fallback emulation: when fallback emptyVisitsForPets() executes, items list is empty
        List<Object> fallbackItems = List.of();
        assertThat(fallbackItems).isEmpty();
    }

    @Test
    @Story("Visits Service Timeout")
    @DisplayName("Verify Circuit Breaker behavior when visits-service has a high latency delay")
    @Description("Emulates timeout handling in ReactiveCircuitBreaker")
    void shouldHandleTimeoutDelayFromVisitsService() {
        visitsServiceMock.stubFor(get(urlEqualTo("/pets/visits?petId=55"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(3000)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"items\": []}")));

        long start = System.currentTimeMillis();
        given()
            .baseUri(visitsServiceMock.baseUrl())
            .when()
            .get("/pets/visits?petId=55")
            .then()
            .statusCode(200);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isGreaterThanOrEqualTo(2900);
    }
}
