package org.springframework.samples.petclinic.test.e2e.ui;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.test.core.model.dto.SpecialtyDto;
import org.springframework.samples.petclinic.test.core.model.dto.VetDto;
import org.springframework.samples.petclinic.test.e2e.BaseE2ETest;
import org.springframework.samples.petclinic.test.e2e.pages.VetsPage;
import org.springframework.samples.petclinic.test.e2e.pages.VetsPage.VetRow;
import org.springframework.samples.petclinic.test.e2e.pages.WelcomePage;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Epic("E2E Tests")
@Feature("UI Veterinarians page")
@Tag("ui")
class VetsPageUiE2ETest extends BaseE2ETest {

    private static List<VetRow> expectedRows(List<VetDto> vets) {
        return vets.stream()
            .map(vet -> new VetRow(
                vet.firstName() + " " + vet.lastName(),
                vet.specialties().isEmpty()
                    ? "none"
                    : vet.specialties().stream().map(SpecialtyDto::name).sorted().collect(Collectors.joining(", "))))
            .toList();
    }

    @Test
    @DisplayName("The page lists every veterinarian of the database with the specialties, or \"none\"")
    void vetsPageMatchesDatabase() {
        List<VetDto> inDatabase = vetDao.findAllWithSpecialties();
        assertThat(inDatabase).isNotEmpty();

        List<VetRow> shown = new VetsPage(page).open().getVets();

        assertThat(shown).containsExactlyInAnyOrderElementsOf(expectedRows(inDatabase));
    }

    @Test
    @Tag("hybrid")
    @DisplayName("The page shows the same veterinarians as the API")
    void vetsPageMatchesApi() {
        List<VetDto> fromApi = petClinicClient.getVetDtos();

        List<VetRow> shown = new VetsPage(page).open().getVets();

        assertThat(shown).containsExactlyInAnyOrderElementsOf(expectedRows(fromApi));
    }

    @Test
    @DisplayName("The Veterinarians link in the navigation bar opens the list")
    void navigationBarOpensVets() {
        new WelcomePage(page).open();
        VetsPage vetsPage = new VetsPage(page).openFromNavigation();

        assertThat(vetsPage.getVets()).isNotEmpty();
        assertThat(vetsPage.currentPath()).isEqualTo("/vets");
    }

    @Test
    @Tag("negative")
    @DisplayName("When the vets service is unavailable the page says so instead of showing an empty table")
    void unavailableServiceIsReported() {
        // Simulates the gateway answering 503 for this one request; the stack itself stays untouched.
        page.route("**/api/vet/vets", route -> route.fulfill(
            new com.microsoft.playwright.Route.FulfillOptions().setStatus(503).setBody("unavailable")));
        VetsPage vetsPage = new VetsPage(page);

        vetsPage.open();

        assertThat(vetsPage.loadFailureTitle()).contains("temporarily unavailable");
        assertThat(vetsPage.isTableShown()).isFalse();
    }
}
