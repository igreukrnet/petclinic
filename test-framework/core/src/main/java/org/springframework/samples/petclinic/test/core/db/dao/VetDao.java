package org.springframework.samples.petclinic.test.core.db.dao;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.springframework.samples.petclinic.test.core.db.mapper.SpecialtyRowMapper;
import org.springframework.samples.petclinic.test.core.db.mapper.VetRowMapper;
import org.springframework.samples.petclinic.test.core.model.dto.SpecialtyDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;

import java.util.ArrayList;
import java.util.List;

public interface VetDao {

    @SqlQuery("SELECT id, first_name, last_name FROM vets ORDER BY last_name")
    @RegisterRowMapper(VetRowMapper.class)
    List<VetDto> findAllVets();

    @SqlQuery("SELECT s.id, s.name " +
              "FROM specialties s " +
              "JOIN vet_specialties vs ON s.id = vs.specialty_id " +
              "WHERE vs.vet_id = :vetId " +
              "ORDER BY s.name")
    @RegisterRowMapper(SpecialtyRowMapper.class)
    List<SpecialtyDto> findSpecialtiesByVetId(@Bind("vetId") int vetId);

    default List<VetDto> findAllWithSpecialties() {
        List<VetDto> vets = findAllVets();
        List<VetDto> result = new ArrayList<>(vets.size());
        for (VetDto vet : vets) {
            List<SpecialtyDto> specs = findSpecialtiesByVetId(vet.id());
            result.add(new VetDto(vet.id(), vet.firstName(), vet.lastName(), specs));
        }
        return result;
    }
}
