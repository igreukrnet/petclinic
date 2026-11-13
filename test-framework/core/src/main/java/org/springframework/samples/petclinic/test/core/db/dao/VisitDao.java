package org.springframework.samples.petclinic.test.core.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.customizer.BindMethods;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.springframework.samples.petclinic.test.core.db.mapper.VisitRowMapper;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.util.List;
import java.util.Optional;

@RegisterRowMapper(VisitRowMapper.class)
public interface VisitDao {

    @SqlQuery("SELECT id, pet_id, visit_date, description FROM visits WHERE id = :id")
    Optional<VisitDto> findById(@Bind("id") int id);

    @SqlQuery("SELECT id, pet_id, visit_date, description FROM visits WHERE pet_id = :petId ORDER BY visit_date")
    List<VisitDto> findByPetId(@Bind("petId") int petId);

    @SqlQuery("SELECT id, pet_id, visit_date, description FROM visits WHERE pet_id IN (<petIds>) ORDER BY visit_date")
    List<VisitDto> findByPetIds(@BindList("petIds") List<Integer> petIds);

    @SqlUpdate("INSERT INTO visits (pet_id, visit_date, description) " +
               "VALUES (:petId, :req.date, :req.description)")
    @GetGeneratedKeys("id")
    int insert(@Bind("petId") int petId, @BindMethods("req") VisitRequest request);

    @SqlUpdate("INSERT INTO visits (pet_id, visit_date, description) " +
               "VALUES (:petId, :date, :description)")
    @GetGeneratedKeys("id")
    int insertFields(
        @Bind("petId") int petId,
        @Bind("date") String date,
        @Bind("description") String description
    );

    @SqlUpdate("DELETE FROM visits WHERE id = :id")
    void deleteById(@Bind("id") int id);

    @SqlUpdate("DELETE FROM visits WHERE pet_id = :petId")
    void deleteByPetId(@Bind("petId") int petId);
}
