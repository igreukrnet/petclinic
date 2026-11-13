package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.OwnerDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;

public class OwnerRowMapper implements RowMapper<OwnerDto> {

    @Override
    public OwnerDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new OwnerDto(
            rs.getInt("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("address"),
            rs.getString("city"),
            rs.getString("telephone"),
            Collections.emptyList()
        );
    }
}
