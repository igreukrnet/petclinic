package org.springframework.samples.petclinic.test.core.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.config.TestConfig;
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

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

public final class ConnectionFactory {

    private static volatile Jdbi defaultJdbi;
    private static final ConcurrentHashMap<String, Jdbi> JDBI_CACHE = new ConcurrentHashMap<>();

    private ConnectionFactory() {}

    public static Jdbi getDefaultJdbi() {
        if (defaultJdbi == null) {
            synchronized (ConnectionFactory.class) {
                if (defaultJdbi == null) {
                    TestConfig config = ConfigurationManager.getConfig();
                    defaultJdbi = createJdbi(config.dbUrl(), config.dbUser(), config.dbPassword());
                }
            }
        }
        return defaultJdbi;
    }

    public static Jdbi createJdbi(String jdbcUrl, String username, String password) {
        String cacheKey = jdbcUrl + "|" + username;
        return JDBI_CACHE.computeIfAbsent(cacheKey, key -> {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(60000);
            hikariConfig.setMaxLifetime(1800000);

            DataSource dataSource = new HikariDataSource(hikariConfig);
            return createJdbi(dataSource);
        });
    }

    public static Jdbi createJdbi(DataSource dataSource) {
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

    public static OwnerDao getOwnerDao() {
        return getDefaultJdbi().onDemand(OwnerDao.class);
    }

    public static OwnerDao getOwnerDao(Jdbi jdbi) {
        return jdbi.onDemand(OwnerDao.class);
    }

    public static PetDao getPetDao() {
        return getDefaultJdbi().onDemand(PetDao.class);
    }

    public static PetDao getPetDao(Jdbi jdbi) {
        return jdbi.onDemand(PetDao.class);
    }

    public static VisitDao getVisitDao() {
        return getDefaultJdbi().onDemand(VisitDao.class);
    }

    public static VisitDao getVisitDao(Jdbi jdbi) {
        return jdbi.onDemand(VisitDao.class);
    }

    public static VetDao getVetDao() {
        return getDefaultJdbi().onDemand(VetDao.class);
    }

    public static VetDao getVetDao(Jdbi jdbi) {
        return jdbi.onDemand(VetDao.class);
    }
}
