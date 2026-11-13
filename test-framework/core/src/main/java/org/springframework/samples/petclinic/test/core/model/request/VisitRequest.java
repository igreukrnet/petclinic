package org.springframework.samples.petclinic.test.core.model.request;

public record VisitRequest(
    String date,
    String description
) {}
