package org.springframework.samples.petclinic.test.core.api;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;

public class ApiClient {

    private final String baseUrl;

    public ApiClient() {
        this(ConfigurationManager.getConfig().gatewayUrl());
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public RequestSpecification getSpec() {
        return new RequestSpecBuilder()
            .setBaseUri(baseUrl)
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .addFilter(new ResponseLoggingFilter(LogDetail.ALL))
            .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
