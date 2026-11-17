package org.springframework.samples.petclinic.test.core.api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.config.TestConfig;

/**
 * Holds the request specification for one base address: JSON, timeouts, and the Allure filter that
 * attaches every request and response to the report.
 * <p>
 * Timeouts are set so that a hanging gateway fails the test in seconds instead of blocking the run.
 * The console gets the request and response only when a REST Assured validation fails; the full
 * exchange is always in the Allure report.
 */
public class ApiClient {

    private final String baseUrl;
    private final RequestSpecification spec;

    public ApiClient() {
        this(ConfigurationManager.getConfig().gatewayUrl());
    }

    public ApiClient(String baseUrl) {
        TestConfig config = ConfigurationManager.getConfig();
        this.baseUrl = baseUrl;
        this.spec = new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .setConfig(RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", config.httpConnectTimeoutMs())
                    .setParam("http.socket.timeout", config.httpReadTimeoutMs()))
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL)))
            .addFilter(new AllureRestAssured())
            .build();
    }

    /** The shared specification. Callers use it through {@code given().spec(...)}, which copies it. */
    public RequestSpecification getSpec() {
        return spec;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
