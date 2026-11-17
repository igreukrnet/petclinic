package org.springframework.samples.petclinic.test.e2e;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.samples.petclinic.test.ai.AiFailureTriageExtension;
import org.springframework.samples.petclinic.test.core.api.PetClinicClient;
import org.springframework.samples.petclinic.test.core.config.ConfigurationManager;
import org.springframework.samples.petclinic.test.core.config.TestConfig;
import org.springframework.samples.petclinic.test.core.db.Database;
import org.springframework.samples.petclinic.test.core.db.dao.OwnerDao;
import org.springframework.samples.petclinic.test.core.db.dao.PetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VetDao;
import org.springframework.samples.petclinic.test.core.db.dao.VisitDao;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Base of every E2E test: the API client, the database and the cleanup of created owners.
 * It starts no browser. UI tests extend {@link BaseUiE2ETest}.
 * <p>
 * All state is held in instance fields (one instance per test class), so test classes do not share anything.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(AiFailureTriageExtension.class)
public abstract class BaseE2ETest {

    protected TestConfig config;
    protected PetClinicClient petClinicClient;
    protected Database database;
    protected OwnerDao ownerDao;
    protected PetDao petDao;
    protected VisitDao visitDao;
    protected VetDao vetDao;

    /** Owners created by the current test. Deleted in {@link #deleteTrackedOwners()} even when the test fails. */
    private final List<Integer> createdOwnerIds = new ArrayList<>();

    @BeforeAll
    void connectToApiAndDatabase() {
        config = ConfigurationManager.getConfig();
        petClinicClient = new PetClinicClient(config.gatewayUrl());
        database = Database.fromConfig();
        ownerDao = database.owners();
        petDao = database.pets();
        visitDao = database.visits();
        vetDao = database.vets();
    }

    /**
     * Runs even when the test failed, and after the {@code @AfterEach} methods of subclasses (JUnit runs
     * those first), so the browser is already closed. Cleanup failures are reported as failures of the test:
     * leftover data would break later tests, so they must not be swallowed.
     */
    @AfterEach
    void deleteTrackedOwners() {
        List<Integer> ids = List.copyOf(createdOwnerIds);
        createdOwnerIds.clear();

        List<RuntimeException> failures = new ArrayList<>();
        for (int ownerId : ids) {
            try {
                deleteOwnerCascade(ownerId);
            } catch (RuntimeException e) {
                failures.add(e);
            }
        }
        if (!failures.isEmpty()) {
            IllegalStateException error = new IllegalStateException(
                "Cleanup failed for owner ids " + ids + "; database may contain leftover rows");
            failures.forEach(error::addSuppressed);
            throw error;
        }
    }

    @AfterAll
    void closeDatabase() {
        if (database != null) {
            database.close();
        }
    }

    /** Registers an owner for deletion at the end of the current test. Call it right after the owner is created. */
    protected void trackOwner(int ownerId) {
        createdOwnerIds.add(ownerId);
    }

    /** Creates an owner through the API for test setup and tracks it for cleanup. Fails if the owner is not created. */
    protected int createOwnerViaApi(OwnerRequest request) {
        int ownerId = petClinicClient.createOwner(request).then().statusCode(201).extract().jsonPath().getInt("id");
        trackOwner(ownerId);
        return ownerId;
    }

    /** Adds a pet through the API for test setup. Fails if the pet is not created. */
    protected int addPetViaApi(int ownerId, PetRequest request) {
        return petClinicClient.addPet(ownerId, request).then().statusCode(201).extract().jsonPath().getInt("id");
    }

    /** Adds a visit through the API for test setup. Fails if the visit is not created. */
    protected int addVisitViaApi(int ownerId, int petId, VisitRequest request) {
        return petClinicClient.addVisit(ownerId, petId, request).then().statusCode(201).extract().jsonPath().getInt("id");
    }

    /** Deletes an owner with its pets and visits in one transaction. Throws on failure. */
    protected void deleteOwnerCascade(int ownerId) {
        database.deleteOwnerCascade(ownerId);
    }
}
