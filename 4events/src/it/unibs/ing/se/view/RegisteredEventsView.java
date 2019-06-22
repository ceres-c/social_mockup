package it.unibs.ing.se.view;

import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class RegisteredEventsView extends AbstractEventsView {
    UUID currentUserID;

    public RegisteredEventsView(UUID currentUserID) {
        this.currentUserID = currentUserID;
    }

    @Override
    public void createWorkingSet() {
        try {
            eventIDs = dbConnection.getEventsByRegistration(currentUserID);
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        } catch (NoSuchElementException e) {
            eventIDs = null;
        }
    }
}
