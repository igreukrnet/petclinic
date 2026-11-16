package org.springframework.samples.petclinic.test.core.model.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

/**
 * Reads a calendar date from the two formats the API uses today:
 * <ul>
 *   <li>{@code yyyy-MM-dd}, for example visit dates and pet birth dates in the database;</li>
 *   <li>an ISO date-time with an offset, for example {@code 2000-09-07T00:00:00.000Z}, which
 *       customers-service returns for pet birth dates.</li>
 * </ul>
 * For a date-time the date is taken as the server wrote it, in the offset it carries. It is not
 * converted to UTC first, because that would shift the day for offsets that are ahead of UTC.
 * <p>
 * Any other format fails with a message that includes the raw value.
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) {
            return null;
        }
        return parse(text.trim());
    }

    /** Package-visible for unit tests. */
    static LocalDate parse(String text) {
        try {
            return text.length() == 10 ? LocalDate.parse(text) : OffsetDateTime.parse(text).toLocalDate();
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Unsupported date format: '" + text + "'", e);
        }
    }
}
