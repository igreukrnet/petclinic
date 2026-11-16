package org.springframework.samples.petclinic.test.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.test.ai.AiFailureTriageExtension;
import org.springframework.samples.petclinic.test.core.db.Database;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Starts MySQL in Testcontainers and applies {@code /db/init.sql}.
 * <p>
 * The container is shared by all subclasses (reuse is enabled) and is skipped with a clear message
 * when Docker is not available, instead of failing during class initialization.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public abstract class BaseComponentTest {

    private static final Logger log = LoggerFactory.getLogger(BaseComponentTest.class);

    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.5")
        .withDatabaseName("petclinic")
        .withUsername("root")
        .withPassword("petclinic")
        .withReuse(true);

    protected Database database;
    protected OwnerDao ownerDao;
    protected PetDao petDao;
    protected VisitDao visitDao;
    protected VetDao vetDao;

    @BeforeAll
    void initDatabase() throws IOException {
        database = Database.connect(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        ownerDao = database.owners();
        petDao = database.pets();
        visitDao = database.visits();
        vetDao = database.vets();
        applyInitScript();
    }

    @AfterAll
    void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    private void applyInitScript() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/db/init.sql")) {
            if (is == null) {
                throw new IllegalStateException("/db/init.sql not found on the test classpath");
            }
            String script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (var handle = database.jdbi().open()) {
                handle.createScript(script).execute();
                log.info("Database schema initialized from /db/init.sql");
            }
        }
    }
}
