package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.PetDto;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

public class PetRowMapper implements RowMapper<PetDto> {

    @Override
    public PetDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        Date birthDate = rs.getDate("birth_date");
        String birthDateStr = birthDate != null ? birthDate.toString() : null;

        PetTypeDto type = null;
        try {
            int typeId = rs.getInt("type_id");
            String typeName = rs.getString("type_name");
            if (typeName != null || typeId > 0) {
                type = new PetTypeDto(typeId, typeName);
            }
        } catch (SQLException ignored) {
            try {
                int typeId = rs.getInt("type_id");
                if (typeId > 0) {
                    type = new PetTypeDto(typeId, null);
                }
            } catch (SQLException ignored2) {
                // Ignore if type_id is not present
            }
        }

        return new PetDto(
            rs.getInt("id"),
            rs.getString("name"),
            birthDateStr,
            type,
            Collections.emptyList()
        );
    }
}
