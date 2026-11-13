package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.SpecialtyDto;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SpecialtyRowMapper implements RowMapper<SpecialtyDto> {

    @Override
    public SpecialtyDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new SpecialtyDto(
            rs.getInt("id"),
            rs.getString("name")
        );
    }
}
