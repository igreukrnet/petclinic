package org.springframework.samples.petclinic.test.core.model.json;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexibleLocalDateDeserializerTest {

    @Test
    void parsesPlainDate() {
        assertThat(FlexibleLocalDateDeserializer.parse("2000-09-07")).isEqualTo(LocalDate.of(2000, 9, 7));
    }

    @Test
    void parsesUtcDateTimeAsWrittenByServer() {
        assertThat(FlexibleLocalDateDeserializer.parse("2000-09-07T00:00:00.000Z"))
            .isEqualTo(LocalDate.of(2000, 9, 7));
    }

    @Test
    void keepsDateOfPositiveOffsetInsteadOfShiftingToUtc() {
        // Midnight in +03:00 is the previous day in UTC; the date the server wrote must be kept.
        assertThat(FlexibleLocalDateDeserializer.parse("2000-09-07T00:00:00.000+03:00"))
            .isEqualTo(LocalDate.of(2000, 9, 7));
    }

    @Test
    void keepsDateOfNegativeOffsetInsteadOfShiftingToUtc() {
        // 23:30 in -05:00 is already the next day in UTC; the date the server wrote must be kept.
        assertThat(FlexibleLocalDateDeserializer.parse("2000-09-07T23:30:00-05:00"))
            .isEqualTo(LocalDate.of(2000, 9, 7));
    }

    @Test
    void rejectsUnknownFormatAndIncludesRawValue() {
        assertThatThrownBy(() -> FlexibleLocalDateDeserializer.parse("07.09.2000"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("07.09.2000");
    }
}
