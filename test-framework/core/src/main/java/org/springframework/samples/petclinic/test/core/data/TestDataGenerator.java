package org.springframework.samples.petclinic.test.core.data;

import net.datafaker.Faker;
import org.springframework.samples.petclinic.test.core.model.request.OwnerRequest;
import org.springframework.samples.petclinic.test.core.model.request.PetRequest;
import org.springframework.samples.petclinic.test.core.model.request.VisitRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TestDataGenerator {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static OwnerRequest generateOwnerRequest() {
        String firstName = FAKER.name().firstName().replaceAll("[^a-zA-Z]", "");
        String lastName = FAKER.name().lastName().replaceAll("[^a-zA-Z]", "");
        String address = FAKER.address().streetAddress();
        String city = FAKER.address().city();
        // Strict digits up to 12 chars: @Digits(fraction = 0, integer = 12), ensure starting with non-zero digit
        String telephone = String.valueOf(FAKER.number().numberBetween(1000000000L, 9999999999L));

        return new OwnerRequest(firstName, lastName, address, city, telephone);
    }

    public static OwnerRequest generateOwnerRequest(String lastName) {
        String firstName = FAKER.name().firstName().replaceAll("[^a-zA-Z]", "");
        String cleanLastName = lastName.replaceAll("[^a-zA-Z]", "");
        String address = FAKER.address().streetAddress();
        String city = FAKER.address().city();
        String telephone = String.valueOf(FAKER.number().numberBetween(1000000000L, 9999999999L));

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
        // Types 1..6: 1=cat, 2=dog, 3=lizard, 4=snake, 5=bird, 6=hamster
        int typeId = FAKER.number().numberBetween(1, 6);
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
