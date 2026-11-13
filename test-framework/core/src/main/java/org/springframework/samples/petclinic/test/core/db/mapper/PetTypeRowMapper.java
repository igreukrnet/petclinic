package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.PetTypeDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PetTypeRowMapper implements RowMapper<PetTypeDto> {

    @Override
    public PetTypeDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new PetTypeDto(
            rs.getInt("id"),
            rs.getString("name")
        );
    }
}
