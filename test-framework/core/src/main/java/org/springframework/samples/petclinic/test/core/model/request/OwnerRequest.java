package org.springframework.samples.petclinic.test.core.model.request;

public record OwnerRequest(
    String firstName,
    String lastName,
    String address,
    String city,
    String telephone
) {}
