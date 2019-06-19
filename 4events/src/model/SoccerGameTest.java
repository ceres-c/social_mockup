package model;

import model.fields.Sex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static menu.Menu.SHA512PasswordHash;
import static menu.Menu.charArrayToByteArray;
import static org.junit.jupiter.api.Assertions.*;

class SoccerGameTest {

    private Event event = new SoccerGame(/* EventID */UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), /* CreatorID */UUID.fromString("123e4567-e89b-12d3-a456-556642440000"));
    LocalDateTime currentDate = LocalDateTime.now();

    SoccerGameTest () {
        event.setAttribute("title", "Titolo Evento");
        event.setAttribute("participantsMin", 11);
        event.setAttribute("registrationDeadline", LocalDateTime.parse("2019-12-01T10:00:00"));
        event.setAttribute("location", "Luogo Evento");
        event.setAttribute("startDate", LocalDateTime.parse("2019-12-31T08:00:00"));
        event.setAttribute("duration", Duration.parse("PT2H"));
        event.setAttribute("cost", 10.0);
        event.setAttribute("inQuota", "Compreso nel prezzo Evento");
        event.setAttribute("endDate", LocalDateTime.parse("2019-12-31T10:00:00"));
        event.setAttribute("notes", "Note Evento");
        event.setAttribute("gender", new Sex("M"));
        event.setAttribute("ageMin", 20);
        event.setAttribute("ageMax", 30);
    }

    @Test
    void registerUser() {
        final String username = "user001";
        final char[] password = {'i', 'l', 'o', 'v', 'e', 'y', 'o', 'u'}; // rockyou.txt docet
        byte[] salt = charArrayToByteArray(username.toCharArray());
        String hashedPassword = SHA512PasswordHash(password, salt);
        final Sex gender = new Sex("M");
        final Integer age = 25;
        User user = new User(username, hashedPassword, gender, age, null); // favoriteCategories not specified

        assertTrue(event.register(user));
    }

    @Test
    void registerUUID() {
        assertTrue(event.register(UUID.randomUUID()));
    }

    @Test
    void publish() {
        event.publish(currentDate);
    }

    /**
     * Event is initialized with State.UNKNOWN state
     */
    @Test
    void stateUnknown() {
        assertEquals(event.getCurrentStateAsString(), "UNKNOWN");
    }

    /**
     * if an Event in UNKNOWN state is legal, then it's set as VALID
     */
    @Test
    void StateValid() {
        Assertions.assertAll(
                () -> assertTrue(event.updateState(LocalDateTime.now())), // Date not relevant for this Test
                () -> assertEquals(event.getCurrentStateAsString(), "VALID")
        );
    }

    /**
     * if an Event is VALID and it gets published, then it's set as OPEN
     */
    @Test
    void StateOpen() {
        event.updateState(LocalDateTime.now()); // UNKNOWN -> VALID
        Assertions.assertAll(
                () -> assertTrue(event.updateState(LocalDateTime.parse("2019-12-01T09:00:00"))), // Same date, 1 hour before the registrationDeadline
                () -> assertEquals(event.getCurrentStateAsString(), "OPEN")
        );
    }

    @Test
    void StateClosed() {
        event.updateState(LocalDateTime.now()); // UNKNOWN -> VALID
        event.publish(currentDate);
        event.updateState(LocalDateTime.parse("2019-12-01T09:00:00")); // VALID -> OPEN: Same date, 1 hour before the registrationDeadline
        for (int i = 0; i < 11; i++)
            event.register(UUID.randomUUID());
        event.updateState(LocalDateTime.parse("2019-12-01T09:00:00")); // OPEN -> CLOSED
        assertEquals(event.getCurrentStateAsString(), "CLOSED");
    }

    @Test
    void StateFailed() {
        event.updateState(LocalDateTime.now()); // UNKNOWN -> VALID
        event.publish(currentDate);
        event.updateState(LocalDateTime.parse("2019-12-01T09:00:00")); // VALID -> OPEN: Same date, 1 hour before the registrationDeadline
        for (int i = 0; i < 5; i++) // Less users than needed
            event.register(UUID.randomUUID());
        event.updateState(LocalDateTime.parse("2019-12-01T11:00:00")); // OPEN -> FAILED: Same date, 1 hour after registrationDeadline
        assertEquals(event.getCurrentStateAsString(), "FAILED");
    }

    @Test
    void StateEnded() {
        event.updateState(LocalDateTime.now()); // UNKNOWN -> VALID
        event.publish(currentDate);
        event.updateState(LocalDateTime.parse("2019-12-01T09:00:00")); // VALID -> OPEN: Same date, 1 hour before the registrationDeadline
        for (int i = 0; i < 11; i++)
            event.register(UUID.randomUUID());
        event.updateState(LocalDateTime.parse("2019-12-01T09:00:00")); // OPEN -> CLOSED
        event.updateState(LocalDateTime.parse("2020-01-01T00:00:00")); // CLOSED -> ENDED: 1 Day after endDate
        assertEquals(event.getCurrentStateAsString(), "ENDED");
    }
}