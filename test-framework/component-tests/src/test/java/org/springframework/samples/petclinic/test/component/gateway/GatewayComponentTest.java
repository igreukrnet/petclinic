package org.springframework.samples.petclinic.test.component.gateway;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

/**
 * Routing, fallback, retry and the owner aggregate of the real api-gateway against stubbed downstream services.
 * The gateway keeps its default circuit-breaker settings here, so no test in this class opens a circuit
 * (circuit behaviour is in {@link GatewayCircuitBreakerComponentTest}).
 */
@Epic("API gateway")
@Feature("Routing and aggregation")
@Tag("component")
class GatewayComponentTest extends BaseGatewayTest {

    private static final String OWNER_WITH_TWO_PETS = """
        {"id":1,"firstName":"Anna","lastName":"Smith","address":"1 Main St","city":"Kyiv","telephone":"1234567890",
         "pets":[
           {"id":7,"name":"Tom","birthDate":"2020-01-01","type":{"id":1,"name":"cat"},"visits":[]},
           {"id":8,"name":"Rex","birthDate":"2019-05-05","type":{"id":2,"name":"dog"},"visits":[]}]}
        """;

    private RequestSpecification gateway;

    @BeforeAll
    void startSharedGateway() {
        gateway = startGateway();
    }

    // ---- routing ----

    @Test
    @DisplayName("/api/customer/** is forwarded to customers-service without the /api/customer prefix")
    void customerRouteStripsPrefix() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(okJson(OWNER_WITH_TWO_PETS)));

        given(gateway).get("/api/customer/owners/1")
            .then().statusCode(200)
            .body("lastName", org.hamcrest.Matchers.equalTo("Smith"));

        customers.verify(1, getRequestedFor(urlEqualTo("/owners/1")));
    }

    @Test
    @DisplayName("/api/vet/** is forwarded to vets-service without the /api/vet prefix")
    void vetRouteStripsPrefix() {
        vets.stubFor(get(urlEqualTo("/vets")).willReturn(okJson("[{\"id\":1,\"firstName\":\"James\"}]")));

        given(gateway).get("/api/vet/vets")
            .then().statusCode(200)
            .body("", hasSize(1))
            .body("[0].firstName", org.hamcrest.Matchers.equalTo("James"));

        vets.verify(1, getRequestedFor(urlEqualTo("/vets")));
    }

    @Test
    @DisplayName("/api/visit/** is forwarded to visits-service without the /api/visit prefix, query string included")
    void visitRouteStripsPrefixAndKeepsQuery() {
        visits.stubFor(get(urlPathEqualTo("/pets/visits")).withQueryParam("petId", equalTo("7,8"))
            .willReturn(okJson("{\"items\":[]}")));

        given(gateway).get("/api/visit/pets/visits?petId=7,8").then().statusCode(200);

        visits.verify(1, getRequestedFor(urlPathEqualTo("/pets/visits")).withQueryParam("petId", equalTo("7,8")));
    }

    @Test
    @DisplayName("A POST keeps its body and the downstream status and body are returned unchanged")
    void postBodyAndCreatedStatusPassThrough() {
        customers.stubFor(post(urlEqualTo("/owners"))
            .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json").withBody("{\"id\":42}")));

        given(gateway).body("{\"firstName\":\"Anna\"}").post("/api/customer/owners")
            .then().statusCode(201)
            .body("id", org.hamcrest.Matchers.equalTo(42));

        customers.verify(1, postRequestedFor(urlEqualTo("/owners")).withRequestBody(equalToJson("{\"firstName\":\"Anna\"}")));
    }

    @Tag("negative")
    @Test
    @DisplayName("A 404 from the downstream service is returned as 404, not as a fallback")
    void notFoundPassesThrough() {
        customers.stubFor(get(urlEqualTo("/owners/99999")).willReturn(aResponse().withStatus(404)));

        given(gateway).get("/api/customer/owners/99999").then().statusCode(404);
    }

    @Tag("negative")
    @Test
    @DisplayName("A 400 from the downstream service is returned as 400 with its body")
    void badRequestPassesThrough() {
        customers.stubFor(post(urlEqualTo("/owners"))
            .willReturn(aResponse().withStatus(400).withBody("invalid owner")));

        given(gateway).body("{}").post("/api/customer/owners")
            .then().statusCode(400)
            .body(org.hamcrest.Matchers.equalTo("invalid owner"));
    }

    // ---- fallback and retry ----

    @Tag("negative")
    @Test
    @DisplayName("An unreachable downstream service gives 503 with the fallback message")
    void unreachableDownstreamGivesFallback() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        given(gateway).get("/api/customer/owners/1")
            .then().statusCode(503)
            .body(containsString("currently unavailable"));
    }

    @Tag("negative")
    @Test
    @DisplayName("A GET that gets 503 is not retried")
    void getIsNotRetriedOn503() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withStatus(503)));

        given(gateway).get("/api/customer/owners/1").then().statusCode(503);

        customers.verify(1, getRequestedFor(urlEqualTo("/owners/1")));
    }

    // ---- owner aggregate ----

    @Test
    @DisplayName("GET /api/gateway/owners/{id} puts each visit on the right pet")
    void aggregateAssignsVisitsToPets() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(okJson(OWNER_WITH_TWO_PETS)));
        visits.stubFor(get(urlPathEqualTo("/pets/visits")).willReturn(okJson("""
            {"items":[
              {"id":1,"petId":7,"date":"2024-01-01","description":"first"},
              {"id":2,"petId":8,"date":"2024-02-02","description":"second"},
              {"id":3,"petId":7,"date":"2024-03-03","description":"third"}]}""")));

        given(gateway).get("/api/gateway/owners/1")
            .then().statusCode(200)
            .body("lastName", org.hamcrest.Matchers.equalTo("Smith"))
            .body("pets.find { it.id == 7 }.visits.description", containsInAnyOrder("first", "third"))
            .body("pets.find { it.id == 8 }.visits.description", contains("second"));

        visits.verify(1, getRequestedFor(urlPathEqualTo("/pets/visits")));
    }

    @Tag("negative")
    @Test
    @DisplayName("Aggregate: when visits-service is down the owner is still returned with 200")
    void aggregateSurvivesVisitsOutage() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(okJson(OWNER_WITH_TWO_PETS)));
        visits.stubFor(get(urlPathEqualTo("/pets/visits")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        given(gateway).get("/api/gateway/owners/1")
            .then().statusCode(200)
            .body("id", org.hamcrest.Matchers.equalTo(1))
            .body("pets", hasSize(2));
    }

    // ---- known application defects: these tests fail on purpose until the application is fixed ----

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #6: a POST is not retried: a 503 from the downstream service must not create the owner twice")
    void postIsNotRetried() {
        customers.stubFor(post(urlEqualTo("/owners")).inScenario("retry").whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503)).willSetStateTo("second"));
        customers.stubFor(post(urlEqualTo("/owners")).inScenario("retry").whenScenarioStateIs("second")
            .willReturn(aResponse().withStatus(201).withBody("{}")));

        given(gateway).body("{}").post("/api/customer/owners");

        customers.verify(1, postRequestedFor(urlEqualTo("/owners")));
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #7: a 500 from the downstream service is a failure and gives 503 from the gateway (the gateway passes the 500 on)")
    void downstreamServerErrorGivesServiceUnavailable() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withStatus(500).withBody("boom")));

        given(gateway).get("/api/customer/owners/1").then().statusCode(503);
    }

    @Tag("known-defect")
    @Test
    @DisplayName("Issue #8: a slow downstream service gives 503 after about 3 seconds (the gateway has no timeout and waits for the answer)")
    void slowDownstreamTimesOut() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(okJson(OWNER_WITH_TWO_PETS).withFixedDelay(6000)));

        // The 6 seconds are the simulated slowness of the service, not a wait of the test.
        // The test waits only for the limit it checks: the gateway must give up within 5 seconds.
        CompletableFuture<Integer> status =
            CompletableFuture.supplyAsync(() -> given(gateway).get("/api/customer/owners/1").statusCode());
        try {
            await("the gateway gives up on the slow service")
                .atMost(Duration.ofSeconds(5))
                .until(status::isDone);
            assertThat(status.join()).isEqualTo(503);
        } finally {
            // Let the request end before the next test resets the stubs.
            status.handle((result, error) -> null).join();
        }
    }

    @Tag("negative")
    @Tag("known-defect")
    @Test
    @DisplayName("Issue #12: aggregate, a missing owner is 404 (the gateway answers 500)")
    void aggregateMissingOwnerIsNotFound() {
        customers.stubFor(get(urlEqualTo("/owners/99999")).willReturn(aResponse().withStatus(404)));

        given(gateway).get("/api/gateway/owners/99999").then().statusCode(404);
    }

    @Tag("negative")
    @Tag("known-defect")
    @Test
    @DisplayName("Issue #11: aggregate, when customers-service is down the answer is 503, because the owner is the required part (the gateway answers 500)")
    void aggregateWithoutCustomersIsUnavailable() {
        customers.stubFor(get(urlEqualTo("/owners/1")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        given(gateway).get("/api/gateway/owners/1").then().statusCode(503);
    }
}
