package org.springframework.samples.petclinic.test.component;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.samples.petclinic.test.core.ai.AiFailureTriageExtension;
import org.springframework.samples.petclinic.test.core.db.ConnectionFactory;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public abstract class BaseComponentTest {

    private static final Logger log = LoggerFactory.getLogger(BaseComponentTest.class);

    private static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4.5")
            .withDatabaseName("petclinic")
            .withUsername("root")
            .withPassword("petclinic")
            .withReuse(true);
        MYSQL_CONTAINER.start();
        log.info("Testcontainers MySQL started at {}", MYSQL_CONTAINER.getJdbcUrl());
    }

    protected static Jdbi jdbi;
    protected static OwnerDao ownerDao;
    protected static PetDao petDao;
    protected static VisitDao visitDao;
    protected static VetDao vetDao;

    @BeforeAll
    void initDatabase() throws Exception {
        if (jdbi == null) {
            jdbi = ConnectionFactory.createJdbi(
                MYSQL_CONTAINER.getJdbcUrl(),
                MYSQL_CONTAINER.getUsername(),
                MYSQL_CONTAINER.getPassword()
            );

            ownerDao = ConnectionFactory.getOwnerDao(jdbi);
            petDao = ConnectionFactory.getPetDao(jdbi);
            visitDao = ConnectionFactory.getVisitDao(jdbi);
            vetDao = ConnectionFactory.getVetDao(jdbi);

            try (InputStream is = getClass().getResourceAsStream("/db/init.sql")) {
                if (is != null) {
                    String script = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    try (var handle = jdbi.open()) {
                        handle.createScript(script).execute();
                        log.info("Database schema initialized successfully from /db/init.sql");
                    }
                } else {
                    log.warn("init.sql not found in test resources!");
                }
            }
        }
    }

    public static MySQLContainer<?> getMysqlContainer() {
        return MYSQL_CONTAINER;
    }

    public static Jdbi getJdbi() {
        return jdbi;
    }

    public static OwnerDao getOwnerDao() {
        return ownerDao;
    }

    public static PetDao getPetDao() {
        return petDao;
    }

    public static VisitDao getVisitDao() {
        return visitDao;
    }

    public static VetDao getVetDao() {
        return vetDao;
    }
}
