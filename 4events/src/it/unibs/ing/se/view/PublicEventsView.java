package it.unibs.ing.se.view;

import java.sql.SQLException;
import java.util.NoSuchElementException;

public class PublicEventsView extends AbstractEventsView {
    @Override
    public void createWorkingSet() {
        try {
            eventIDs = dbConnection.getOpenEvents();
        } catch (SQLException e) {
            System.err.println(translation.getTranslation("SQLError"));
            System.exit(1);
        } catch (NoSuchElementException e) {
            eventIDs = null;
        }
    }
}
