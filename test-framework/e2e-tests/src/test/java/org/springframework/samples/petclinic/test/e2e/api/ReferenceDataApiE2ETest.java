package org.springframework.samples.petclinic.test.e2e.api;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.model.dto.SpecialtyDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("Reference data API")
public class ReferenceDataApiE2ETest extends BaseE2ETest {

    @Test
    @Story("Pet types")
    @DisplayName("Pet types from the API are the same rows as in the database")
    void shouldMatchPetTypesWithDatabase() {
        assertThat(petClinicClient.getPetTypeDtos())
            .containsExactlyInAnyOrderElementsOf(petDao.findAllTypes());
    }

    @Test
    @Story("Vets")
    @DisplayName("Vets and their specialties from the API match the database")
    void shouldMatchVetsAndSpecialtiesWithDatabase() {
        List<VetDto> apiVets = petClinicClient.getVetDtos();
        List<VetDto> dbVets = vetDao.findAllWithSpecialties();

        assertThat(apiVets).isNotEmpty();
        assertThat(summarize(apiVets)).isEqualTo(summarize(dbVets));
    }

    /** One line per vet, with its specialties sorted, so both sides compare regardless of order. */
    private static Map<Integer, String> summarize(List<VetDto> vets) {
        return vets.stream().collect(Collectors.toMap(
            VetDto::id,
            vet -> vet.firstName() + " " + vet.lastName() + ": " + vet.specialties().stream()
                .map(SpecialtyDto::name)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining(", ")),
            (a, b) -> { throw new IllegalStateException("Duplicate vet id"); },
            TreeMap::new
        ));
    }
}
