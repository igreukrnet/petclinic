package org.springframework.samples.petclinic.test.core.db.mapper;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.springframework.samples.petclinic.test.core.model.dto.VisitDto;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VisitRowMapper implements RowMapper<VisitDto> {

    @Override
    public VisitDto map(ResultSet rs, StatementContext ctx) throws SQLException {
        Date visitDate = rs.getDate("visit_date");
        String dateStr = visitDate != null ? visitDate.toString() : null;
        return new VisitDto(
            rs.getInt("id"),
            rs.getInt("pet_id"),
            dateStr,
            rs.getString("description")
        );
    }
}
