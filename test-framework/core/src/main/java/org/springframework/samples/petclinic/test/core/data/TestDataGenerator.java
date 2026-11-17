package org.springframework.samples.petclinic.test.core.data;

import net.datafaker.Faker;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class TestDataGenerator {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);
    private static final AtomicLong TELEPHONE_SEQUENCE = new AtomicLong(System.currentTimeMillis());
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * A 10-digit telephone that does not repeat within a run: a counter that starts at the clock, so two
     * runs do not produce the same numbers either. Tests find their owner by telephone, so it must be unique.
     */
    static String uniqueTelephone() {
        long sequence = TELEPHONE_SEQUENCE.incrementAndGet() % 9_000_000_000L;
        return String.valueOf(1_000_000_000L + sequence);
    }

    public static OwnerRequest generateOwnerRequest() {
        String firstName = FAKER.name().firstName().replaceAll("[^a-zA-Z]", "");
        String lastName = FAKER.name().lastName().replaceAll("[^a-zA-Z]", "");
        String address = FAKER.address().streetAddress();
        String city = FAKER.address().city();
        String telephone = uniqueTelephone();

        return new OwnerRequest(firstName, lastName, address, city, telephone);
    }

    public static OwnerRequest generateOwnerRequest(String lastName) {
        String firstName = FAKER.name().firstName().replaceAll("[^a-zA-Z]", "");
        String cleanLastName = lastName.replaceAll("[^a-zA-Z]", "");
        String address = FAKER.address().streetAddress();
        String city = FAKER.address().city();
        String telephone = uniqueTelephone();

        return new OwnerRequest(firstName, cleanLastName, address, city, telephone);
    }

    public static PetRequest generatePetRequest(int typeId) {
        String petName = FAKER.animal().name() + "_" + FAKER.number().digits(4);
        int yearsAgo = FAKER.number().numberBetween(1, 8);
        int daysAgo = FAKER.number().numberBetween(1, 365);
        String birthDate = LocalDate.now().minusYears(yearsAgo).minusDays(daysAgo).format(DATE_FORMAT);

        return new PetRequest(petName, birthDate, typeId);
    }

    public static PetRequest generatePetRequest() {
        // Types 1..6: 1=cat, 2=dog, 3=lizard, 4=snake, 5=bird, 6=hamster.
        // Datafaker's numberBetween excludes the upper bound, so 7 is needed to include hamster (6).
        int typeId = FAKER.number().numberBetween(1, 7);
        return generatePetRequest(typeId);
    }

    public static VisitRequest generateVisitRequest() {
        String visitDate = LocalDate.now().minusDays(FAKER.number().numberBetween(0, 30)).format(DATE_FORMAT);
        String description = "Checkup: " + String.join(" ", FAKER.lorem().words(4)) + " examination";
        if (description.length() > 250) {
            description = description.substring(0, 250);
        }
        return new VisitRequest(visitDate, description);
    }
}
