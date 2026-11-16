package org.springframework.samples.petclinic.test.core.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.samples.petclinic.test.core.model.json.FlexibleLocalDateDeserializer;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisitDto(
    Integer id,
    Integer petId,
    @JsonDeserialize(using = FlexibleLocalDateDeserializer.class)
    LocalDate date,
    String description
) {}
