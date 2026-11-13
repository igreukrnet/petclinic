package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OwnerDto(
    Integer id,
    String firstName,
    String lastName,
    String address,
    String city,
    String telephone,
    List<PetDto> pets
) {
    public OwnerDto {
        if (pets == null) {
            pets = Collections.emptyList();
        }
    }
}
