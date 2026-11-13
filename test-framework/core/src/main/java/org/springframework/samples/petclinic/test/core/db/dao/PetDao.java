package org.springframework.samples.petclinic.test.core.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.springframework.samples.petclinic.test.core.db.mapper.PetRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.PetTypeRowMapper;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(PetRowMapper.class)
public interface PetDao {

    @SqlQuery("SELECT p.id, p.name, p.birth_date, p.type_id, t.name AS type_name " +
              "FROM pets p " +
              "LEFT JOIN types t ON p.type_id = t.id " +
              "WHERE p.id = :id")
    Optional<PetDto> findById(@Bind("id") int id);

    @SqlQuery("SELECT p.id, p.name, p.birth_date, p.type_id, t.name AS type_name " +
              "FROM pets p " +
              "LEFT JOIN types t ON p.type_id = t.id " +
              "WHERE p.owner_id = :ownerId " +
              "ORDER BY p.name")
    List<PetDto> findByOwnerId(@Bind("ownerId") int ownerId);

    @SqlUpdate("INSERT INTO pets (name, birth_date, type_id, owner_id) " +
               "VALUES (:req.name, :req.birthDate, :req.typeId, :ownerId)")
    @GetGeneratedKeys("id")
    int insert(@Bind("ownerId") int ownerId, @BindMethods("req") PetRequest request);

    @SqlUpdate("INSERT INTO pets (name, birth_date, type_id, owner_id) " +
               "VALUES (:name, :birthDate, :typeId, :ownerId)")
    @GetGeneratedKeys("id")
    int insertFields(
        @Bind("name") String name,
        @Bind("birthDate") String birthDate,
        @Bind("typeId") int typeId,
        @Bind("ownerId") int ownerId
    );

    @SqlUpdate("DELETE FROM pets WHERE id = :id")
    void deleteById(@Bind("id") int id);

    @SqlUpdate("DELETE FROM pets WHERE owner_id = :ownerId")
    void deleteByOwnerId(@Bind("ownerId") int ownerId);

    @SqlQuery("SELECT id, name FROM types ORDER BY name")
    @RegisterRowMapper(PetTypeRowMapper.class)
    List<PetTypeDto> findAllTypes();
}
