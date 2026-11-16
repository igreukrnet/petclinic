package org.springframework.samples.petclinic.test.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;
import org.springframework.samples.petclinic.test.core.db.mapper.OwnerRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.PetRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.PetTypeRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.SpecialtyRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.VetRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.VisitRowMapper;

/**
 * A database connection pool with its DAOs. Each test base class creates its own instance,
 * so there is no process-wide state and two databases can be used side by side.
 */
public final class Database implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final Jdbi jdbi;

    private Database(HikariDataSource dataSource) {
        this.dataSource = dataSource;
        this.jdbi = createJdbi(dataSource);
    }

    /** Connects to the database configured in {@code config.properties} or by system property / env. */
    public static Database fromConfig() {
        var config = ConfigurationManager.getConfig();
        return connect(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    public static Database connect(String jdbcUrl, String username, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMaxLifetime(1800000);
        return new Database(new HikariDataSource(hikariConfig));
    }

    public Jdbi jdbi() {
        return jdbi;
    }

    public OwnerDao owners() {
        return jdbi.onDemand(OwnerDao.class);
    }

    public PetDao pets() {
        return jdbi.onDemand(PetDao.class);
    }

    public VisitDao visits() {
        return jdbi.onDemand(VisitDao.class);
    }

    public VetDao vets() {
        return jdbi.onDemand(VetDao.class);
    }

    /**
     * Deletes an owner with its pets and visits in one transaction, in foreign key order.
     * Either everything is deleted or nothing is. Requires the visits table, so it is for the full schema
     * (the E2E stack), not for a customers-service-only database.
     */
    public void deleteOwnerCascade(int ownerId) {
        jdbi.useTransaction(handle -> {
            handle.createUpdate("DELETE v FROM visits v JOIN pets p ON v.pet_id = p.id WHERE p.owner_id = :ownerId")
                .bind("ownerId", ownerId).execute();
            handle.createUpdate("DELETE FROM pets WHERE owner_id = :ownerId")
                .bind("ownerId", ownerId).execute();
            handle.createUpdate("DELETE FROM owners WHERE id = :ownerId")
                .bind("ownerId", ownerId).execute();
        });
    }

    @Override
    public void close() {
        dataSource.close();
    }

    private static Jdbi createJdbi(javax.sql.DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerRowMapper(new OwnerRowMapper());
        jdbi.registerRowMapper(new PetRowMapper());
        jdbi.registerRowMapper(new VisitRowMapper());
        jdbi.registerRowMapper(new VetRowMapper());
        jdbi.registerRowMapper(new SpecialtyRowMapper());
        jdbi.registerRowMapper(new PetTypeRowMapper());
        return jdbi;
    }
}
