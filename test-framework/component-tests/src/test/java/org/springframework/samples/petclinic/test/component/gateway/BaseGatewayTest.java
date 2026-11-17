package org.springframework.samples.petclinic.test.component.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.samples.petclinic.test.component.BaseServiceTest;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Runs the real api-gateway JAR in a container, with WireMock servers (in the test JVM, on dynamic ports)
 * standing in for customers-service, visits-service and vets-service.
 * <p>
 * No Eureka and no config server: the gateway finds the services through a static
 * {@code SimpleDiscoveryClient} list. Everything else comes from the gateway's own {@code application.yml},
 * the same code that runs in the E2E stack. The external config repository of the real stack sets nothing
 * that affects routing, retries, timeouts or circuit breakers (checked in its {@code api-gateway.yml}
 * and {@code application.yml}).
 * <p>
 * Circuit breaker state lives in the gateway, so tests that open a circuit start their own gateway
 * and stop it when the test ends ({@link #stopGateways()}).
 */
public abstract class BaseGatewayTest extends BaseServiceTest {

    private static final int GATEWAY_PORT = 8080;
    private static final String HOST = "http://host.testcontainers.internal:";

    protected WireMockServer customers;
    protected WireMockServer visits;
    protected WireMockServer vets;

    private final List<GenericContainer<?>> gateways = new ArrayList<>();

    @BeforeAll
    void startDownstreamStubs() {
        customers = new WireMockServer(options().dynamicPort());
        visits = new WireMockServer(options().dynamicPort());
        vets = new WireMockServer(options().dynamicPort());
        customers.start();
        visits.start();
        vets.start();
        Testcontainers.exposeHostPorts(customers.port(), visits.port(), vets.port());
    }

    @BeforeEach
    void resetDownstreamStubs() {
        for (WireMockServer server : List.of(customers, visits, vets)) {
            server.resetAll();
        }
    }

    /**
     * Stops every gateway started so far. Classes that start a gateway per test call this after each test,
     * so only one gateway is alive at a time.
     */
    protected void stopGateways() {
        gateways.forEach(GenericContainer::stop);
        gateways.clear();
    }

    @AfterAll
    void stopGatewaysAndStubs() {
        stopGateways();
        for (WireMockServer server : List.of(customers, visits, vets)) {
            if (server != null) {
                server.stop();
            }
        }
    }

    /** Starts a gateway; {@code extraArgs} are Spring command-line properties such as {@code --a.b=c}. */
    protected RequestSpecification startGateway(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of(
            "--spring.cloud.discovery.client.simple.instances.customers-service[0].uri=" + HOST + customers.port(),
            "--spring.cloud.discovery.client.simple.instances.visits-service[0].uri=" + HOST + visits.port(),
            "--spring.cloud.discovery.client.simple.instances.vets-service[0].uri=" + HOST + vets.port(),
            // The E2E stack gets this from the config repository: all actuator endpoints are exposed.
            "--management.endpoints.web.exposure.include=*"));
        args.addAll(List.of(extraArgs));

        GenericContainer<?> gateway = new GenericContainer<>(
            imageFor(resolveJar("gateway.jar",
                "../../app/spring-petclinic-api-gateway/target/spring-petclinic-api-gateway-4.0.1.jar")))
            .withExposedPorts(GATEWAY_PORT)
            .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
            .withEnv("EUREKA_CLIENT_ENABLED", "false")
            .withEnv("SERVER_PORT", String.valueOf(GATEWAY_PORT))
            .withEnv("TZ", "UTC")
            .withCommand(args.toArray(String[]::new))
            .waitingFor(Wait.forHttp("/actuator/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));
        gateway.start();
        gateways.add(gateway);
        return specFor(gateway, GATEWAY_PORT);
    }
}
