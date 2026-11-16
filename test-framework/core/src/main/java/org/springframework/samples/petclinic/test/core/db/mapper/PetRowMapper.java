package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;

public class PetRowMapper implements RowMapper<PetDto> {

    @Override
    public PetDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        Date birthDate = rs.getDate("birth_date");
        LocalDate birthDateValue = birthDate != null ? birthDate.toLocalDate() : null;

        // Every query that uses this mapper selects p.type_id and t.name AS type_name (LEFT JOIN types).
        // A missing column is a broken query and must fail, so SQLException is not caught here.
        // A NULL type name with a non-zero id means the pet refers to a type that does not exist.
        int typeId = rs.getInt("type_id");
        String typeName = rs.getString("type_name");
        PetTypeDto type = (typeName != null || typeId > 0) ? new PetTypeDto(typeId, typeName) : null;

        return new PetDto(
            rs.getInt("id"),
            rs.getString("name"),
            birthDateValue,
            type,
            Collections.emptyList()
        );
    }
}
