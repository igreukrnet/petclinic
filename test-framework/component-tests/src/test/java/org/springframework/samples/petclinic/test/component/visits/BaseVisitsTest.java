package org.springframework.samples.petclinic.test.component.visits;

import org.springframework.samples.petclinic.test.component.BaseComponentTest;
import org.springframework.samples.petclinic.test.core.db.Database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * visits-service in a container. Visits refer to pets, so the schema and seed data of customers-service are
 * loaded first, from the application source files (no copy that could drift).
 */
abstract class BaseVisitsTest extends BaseComponentTest {

    private static final String CUSTOMERS_DB = "../../app/spring-petclinic-customers-service/src/main/resources/db/mysql/";

    @Override
    protected String defaultJarPath() {
        return "../../app/spring-petclinic-visits-service/target/spring-petclinic-visits-service-4.0.1.jar";
    }

    @Override
    protected String readinessPath() {
        return "/actuator/health";
    }

    @Override
    protected void prepareDatabase(Database db) {
        try (var handle = db.jdbi().open()) {
            for (String file : List.of("schema.sql", "data.sql")) {
                String script = Files.readString(Path.of(CUSTOMERS_DB + file), StandardCharsets.UTF_8);
                handle.createScript(script).execute();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the customers-service database scripts", e);
        }
    }
}
