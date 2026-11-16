package org.springframework.samples.petclinic.test.component;

import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.samples.petclinic.test.ai.AiFailureTriageExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Common parts of tests that run a real service JAR from {@code app/} in a container: finding the JAR,
 * building an image from it and creating an HTTP client for the mapped port. Skipped when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public abstract class BaseServiceTest {

    /** The JAR at {@code -Dservice.jar} if set, otherwise at the given default path (relative to the module). */
    protected static Path resolveJar(String defaultPath) {
        return resolveJar("service.jar", defaultPath);
    }

    protected static Path resolveJar(String systemProperty, String defaultPath) {
        Path jar = Path.of(System.getProperty(systemProperty, defaultPath)).toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Service JAR not found: " + jar
                + ". Build the application JARs first: cd app && mvn -B clean package -DskipTests spring-boot:repackage");
        }
        return jar;
    }

    protected static ImageFromDockerfile imageFor(Path jar) {
        return new ImageFromDockerfile()
            .withFileFromPath("application.jar", jar)
            .withDockerfileFromBuilder(b -> b
                .from("eclipse-temurin:17")
                .copy("application.jar", "/application.jar")
                .entryPoint("java", "-jar", "/application.jar")
                .build());
    }

    protected static RequestSpecification specFor(GenericContainer<?> container, int port) {
        return new RequestSpecBuilder()
            .setBaseUri("http://" + container.getHost())
            .setPort(container.getMappedPort(port))
            .setContentType(ContentType.JSON)
            .addFilter(new AllureRestAssured())
            .build();
    }
}
