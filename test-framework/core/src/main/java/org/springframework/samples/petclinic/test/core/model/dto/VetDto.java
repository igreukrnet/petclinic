package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VetDto(
    Integer id,
    String firstName,
    String lastName,
    List<SpecialtyDto> specialties
) {
    public VetDto {
        if (specialties == null) {
            specialties = Collections.emptyList();
        }
    }
}
