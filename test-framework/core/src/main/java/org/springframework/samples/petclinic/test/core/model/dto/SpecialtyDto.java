package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SpecialtyDto(
    Integer id,
    String name
) {}
