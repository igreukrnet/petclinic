package org.springframework.samples.petclinic.test.core.model.request;

public record PetRequest(
    String name,
    String birthDate,
    int typeId
) {}
