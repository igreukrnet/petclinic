package org.springframework.samples.petclinic.test.component.gateway;

import com.github.tomakehurst.wiremock.http.Fault;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The circuit breaker of the real api-gateway. The breaker is made small so that a few calls are enough
 * (window of 4 calls, 5 seconds open, 2 trial calls); everything else is the gateway's own configuration.
 * Each test starts its own gateway, because circuit state lives in the gateway process.
 */
@Epic("API gateway")
@Feature("Circuit breaker")
@Tag("component")
class GatewayCircuitBreakerComponentTest extends BaseGatewayTest {

    private static final int WINDOW = 4;
    private static final Duration OPEN_FOR = Duration.ofSeconds(5);

    private RequestSpecification gateway;

    @BeforeEach
    void startFreshGateway() {
        gateway = startGateway(
            "--resilience4j.circuitbreaker.configs.default.slidingWindowSize=" + WINDOW,
            "--resilience4j.circuitbreaker.configs.default.minimumNumberOfCalls=" + WINDOW,
            "--resilience4j.circuitbreaker.configs.default.waitDurationInOpenState=" + OPEN_FOR.toSeconds() + "s",
            "--resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState=2",
            "--resilience4j.circuitbreaker.instances.defaultCircuitBreaker.slidingWindowSize=" + WINDOW,
            "--resilience4j.circuitbreaker.instances.defaultCircuitBreaker.minimumNumberOfCalls=" + WINDOW,
            "--resilience4j.circuitbreaker.instances.defaultCircuitBreaker.waitDurationInOpenState=" + OPEN_FOR.toSeconds() + "s",
            "--resilience4j.circuitbreaker.instances.defaultCircuitBreaker.permittedNumberOfCallsInHalfOpenState=2");
    }

    private String circuitState() {
        return given(gateway).get("/actuator/circuitbreakers").then().statusCode(200)
            .extract().path("circuitBreakers.defaultCircuitBreaker.state");
    }

    private void callCustomers(int times) {
        for (int i = 0; i < times; i++) {
            given(gateway).get("/api/customer/owners/1");
        }
    }

    private int customersRequests() {
        return customers.countRequestsMatching(getRequestedFor(urlEqualTo("/owners/1")).build()).getCount();
    }

    @Test
    @DisplayName("After enough failures the circuit opens and calls no longer reach the downstream service")
    void circuitOpensAfterFailures() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        callCustomers(WINDOW);
        assertThat(customersRequests()).as("requests that reached customers while closed").isEqualTo(WINDOW);
        assertThat(circuitState()).isEqualTo("OPEN");

        for (int i = 0; i < 5; i++) {
            given(gateway).get("/api/customer/owners/1").then().statusCode(503);
        }
        assertThat(customersRequests()).as("requests that reached customers while open").isEqualTo(WINDOW);
    }

    @Test
    @DisplayName("After the wait the circuit lets a trial call through and closes again when it succeeds")
    void circuitClosesAfterRecovery() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        callCustomers(WINDOW);
        assertThat(circuitState()).isEqualTo("OPEN");

        customers.resetAll();
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(okJson("{\"id\":1}")));

        Instant deadline = Instant.now().plus(OPEN_FOR).plusSeconds(10);
        int status;
        do {
            status = given(gateway).get("/api/customer/owners/1").statusCode();
        } while (status != 200 && Instant.now().isBefore(deadline));
        assertThat(status).as("a call after the open period reaches the recovered service").isEqualTo(200);

        given(gateway).get("/api/customer/owners/1").then().statusCode(200);
        assertThat(circuitState()).isEqualTo("CLOSED");
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #9: failures of customers-service do not break vets-service (the gateway uses one circuit for all services)")
    void circuitIsPerService() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
        vets.stubFor(get(urlEqualTo("/vets")).willReturn(okJson("[]")));
        callCustomers(WINDOW);

        given(gateway).get("/api/vet/vets").then().statusCode(200);
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #10: 500 answers of the downstream service count as failures and open the circuit (the gateway counts only exceptions)")
    void serverErrorsOpenTheCircuit() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withStatus(500)));

        callCustomers(WINDOW);
        given(gateway).get("/api/customer/owners/1");

        assertThat(circuitState()).isEqualTo("OPEN");
        assertThat(customersRequests()).as("requests that reached customers").isEqualTo(WINDOW);
    }
}
