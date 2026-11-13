package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisitDto(
    Integer id,
    Integer petId,
    String date,
    String description
) {}
