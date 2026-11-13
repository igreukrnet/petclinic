package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

public class VetRowMapper implements RowMapper<VetDto> {

    @Override
    public VetDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new VetDto(
            rs.getInt("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            Collections.emptyList()
        );
    }
}
