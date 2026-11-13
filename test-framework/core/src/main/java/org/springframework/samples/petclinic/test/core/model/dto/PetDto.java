package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PetDto(
    Integer id,
    String name,
    String birthDate,
    PetTypeDto type,
    List<VisitDto> visits
) {
    public PetDto {
        if (visits == null) {
            visits = Collections.emptyList();
        }
    }
}
