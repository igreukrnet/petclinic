package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.samples.petclinic.test.core.model.json.FlexibleLocalDateDeserializer;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetDto(
    Integer id,
    String name,
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    LocalDate birthDate,
    PetTypeDto type,
    List<VisitDto> visits
) {
    public PetDto {
        if (visits == null) {
            visits = Collections.emptyList();
        }
    }
}
