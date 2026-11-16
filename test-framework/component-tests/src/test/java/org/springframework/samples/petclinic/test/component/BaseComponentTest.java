package org.springframework.samples.petclinic.test.component;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.samples.petclinic.test.core.db.Database;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;

import java.time.Duration;

/**
 * Runs one real service (the JAR built from {@code app/}) in a container next to MySQL, in a shared network.
 * Tests call the service over HTTP directly (no gateway, no Eureka, no config server) and check the database
 * through the DAOs from {@code core}. The containers are started once per test class and are not reused,
 * so every run starts from the seed data.
 * <p>
 * Subclasses name the service JAR and the readiness path. The JAR must be built first:
 * {@code cd app && mvn -B clean package -DskipTests spring-boot:repackage}.
 * Override the JAR location with {@code -Dservice.jar=...}.
 */
public abstract class BaseComponentTest extends BaseServiceTest {

    private static final String MYSQL_ALIAS = "mysql";
    protected static final int SERVICE_PORT = 8081;

    protected static final Network NETWORK = Network.newNetwork();

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
        .withDatabaseName("petclinic")
        .withUsername("root")
        .withPassword("petclinic")
        .withNetwork(NETWORK)
        .withNetworkAliases(MYSQL_ALIAS);

    protected Database database;
    protected RequestSpecification service;

    private GenericContainer<?> serviceContainer;

    /** Path to the service JAR; the default is relative to the module directory. */
    protected abstract String defaultJarPath();

    /** A path inside the service that answers 200 when the service is ready. */
    protected abstract String readinessPath();

    /** Time zone of the service JVM. UTC by default, as in the E2E stack. */
    protected String timeZone() {
        return "UTC";
    }

    @BeforeAll
    void startServiceAndDatabase() {
        // MYSQL is already started by @Container.
        serviceContainer = createServiceContainer();
        serviceContainer.start();
        database = Database.connect(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        service = specFor(serviceContainer, SERVICE_PORT);
    }

    @AfterAll
    void stopServiceAndDatabase() {
        if (database != null) {
            database.close();
        }
        if (serviceContainer != null) {
            serviceContainer.stop();
        }
    }

    private GenericContainer<?> createServiceContainer() {
        ImageFromDockerfile image = imageFor(resolveJar(defaultJarPath()));
        return new GenericContainer<>(image)
            .withNetwork(NETWORK)
            .withExposedPorts(SERVICE_PORT)
            .withEnv("SPRING_PROFILES_ACTIVE", "mysql")
            .withEnv("SPRING_DATASOURCE_URL",
                "jdbc:mysql://" + MYSQL_ALIAS + ":3306/petclinic?allowPublicKeyRetrieval=true&useSSL=false")
            .withEnv("SPRING_DATASOURCE_USERNAME", MYSQL.getUsername())
            .withEnv("SPRING_DATASOURCE_PASSWORD", MYSQL.getPassword())
            .withEnv("SPRING_CLOUD_CONFIG_ENABLED", "false")
            .withEnv("EUREKA_CLIENT_ENABLED", "false")
            .withEnv("SPRING_SQL_INIT_MODE", "always")
            .withEnv("SPRING_SQL_INIT_SCHEMA_LOCATIONS", "classpath:db/mysql/schema.sql")
            .withEnv("SPRING_SQL_INIT_DATA_LOCATIONS", "classpath:db/mysql/data.sql")
            .withEnv("SERVER_PORT", String.valueOf(SERVICE_PORT))
            .withEnv("TZ", timeZone())
            .waitingFor(Wait.forHttp(readinessPath()).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));
    }
}
