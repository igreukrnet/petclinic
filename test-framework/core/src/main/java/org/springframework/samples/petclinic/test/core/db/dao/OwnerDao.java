package org.springframework.samples.petclinic.test.core.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.springframework.samples.petclinic.test.core.db.mapper.OwnerRowMapper;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(OwnerRowMapper.class)
public interface OwnerDao {

    @SqlQuery("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE id = :id")
    Optional<OwnerDto> findById(@Bind("id") int id);

    @SqlQuery("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE telephone = :telephone")
    Optional<OwnerDto> findByTelephone(@Bind("telephone") String telephone);

    @SqlQuery("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name = :lastName")
    List<OwnerDto> findByLastName(@Bind("lastName") String lastName);

    @SqlQuery("SELECT id, first_name, last_name, address, city, telephone FROM owners ORDER BY id")
    List<OwnerDto> findAll();

    @SqlUpdate("INSERT INTO owners (first_name, last_name, address, city, telephone) " +
               "VALUES (:firstName, :lastName, :address, :city, :telephone)")
    @GetGeneratedKeys("id")
    int insert(@BindMethods OwnerRequest request);

    @SqlUpdate("INSERT INTO owners (first_name, last_name, address, city, telephone) " +
               "VALUES (:firstName, :lastName, :address, :city, :telephone)")
    @GetGeneratedKeys("id")
    int insertFields(
        @Bind("firstName") String firstName,
        @Bind("lastName") String lastName,
        @Bind("address") String address,
        @Bind("city") String city,
        @Bind("telephone") String telephone
    );

    @SqlUpdate("DELETE FROM owners WHERE telephone = :telephone")
    void deleteByTelephone(@Bind("telephone") String telephone);

    @SqlUpdate("DELETE FROM owners WHERE id = :id")
    void deleteById(@Bind("id") int id);

    @SqlQuery("SELECT COUNT(*) FROM owners")
    long count();
}
