/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.api.application;

import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.PetType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

/**
 * @author Maciej Szarlinski
 */
@Component
public class CustomersServiceClient {

    private final WebClient.Builder webClientBuilder;

    public CustomersServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<OwnerDetails> getOwner(final int ownerId) {
        return webClientBuilder.build().get()
            .uri("http://customers-service/owners/{ownerId}", ownerId)
            .retrieve()
            .onStatus(status -> !status.is2xxSuccessful(),
                response -> response.bodyToMono(String.class)
                    .doOnNext(error -> System.err.println("ERROR from customers-service: " + error))
                    .flatMap(error -> Mono.error(new RuntimeException("Failed to fetch owner " + ownerId + ": " + error))))
            .bodyToMono(String.class)
            .doOnNext(body -> System.out.println("Received owner response: " + body))
            .map(this::parseOwnerDetailsFromJson)
            .onErrorResume(e -> {
                System.err.println("Error parsing owner " + ownerId + ": " + e.getMessage());
                e.printStackTrace();
                return Mono.error(e);
            });
    }

    private OwnerDetails parseOwnerDetailsFromJson(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> ownerMap = mapper.readValue(json, java.util.Map.class);
            return convertMapToOwnerDetails(ownerMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse owner JSON: " + json, e);
        }
    }

    private OwnerDetails convertMapToOwnerDetails(java.util.Map<String, Object> ownerMap) {
        try {
            int id = ((Number) ownerMap.get("id")).intValue();
            String firstName = (String) ownerMap.get("firstName");
            String lastName = (String) ownerMap.get("lastName");
            String address = (String) ownerMap.get("address");
            String city = (String) ownerMap.get("city");
            String telephone = (String) ownerMap.get("telephone");
            
            java.util.List<PetDetails> pets = new java.util.ArrayList<>();
            Object petsObj = ownerMap.get("pets");
            if (petsObj instanceof java.util.Collection) {
                java.util.Collection<?> petsCollection = (java.util.Collection<?>) petsObj;
                for (Object petObj : petsCollection) {
                    if (petObj instanceof java.util.Map) {
                        java.util.Map<String, Object> petMap = (java.util.Map<String, Object>) petObj;
                        PetDetails pet = convertMapToPetDetails(petMap);
                        pets.add(pet);
                    }
                }
            }
            
            return new OwnerDetails(id, firstName, lastName, address, city, telephone, pets);
        } catch (Exception e) {
            throw new RuntimeException("Error converting owner map to OwnerDetails", e);
        }
    }

    private PetDetails convertMapToPetDetails(java.util.Map<String, Object> petMap) {
        int id = ((Number) petMap.get("id")).intValue();
        String name = (String) petMap.get("name");
        
        // Handle birthDate - it may come as String, Number (timestamp), or formatted date
        String birthDate = null;
        Object birthDateObj = petMap.get("birthDate");
        if (birthDateObj instanceof String) {
            birthDate = (String) birthDateObj;
        } else if (birthDateObj instanceof Number) {
            // Convert timestamp to date string
            long timestamp = ((Number) birthDateObj).longValue();
            java.time.Instant instant = java.time.Instant.ofEpochMilli(timestamp);
            birthDate = java.time.LocalDate.ofInstant(instant, java.time.ZoneId.systemDefault()).toString();
        }
        
        PetType type = null;
        Object typeObj = petMap.get("type");
        if (typeObj instanceof java.util.Map) {
            java.util.Map<String, Object> typeMap = (java.util.Map<String, Object>) typeObj;
            type = new PetType(
                ((Number) typeMap.get("id")).intValue(),
                (String) typeMap.get("name")
            );
        }
        
        return new PetDetails(id, name, birthDate, type, new java.util.ArrayList<>());
    }
}
