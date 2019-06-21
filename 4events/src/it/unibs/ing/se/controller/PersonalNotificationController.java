package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

public class PersonalNotificationController implements ControllerInterface<ArrayList<UUID>> {
    private Connector dbConnection;

    public PersonalNotificationController () {
        try {
            dbConnection = Connector.getInstance();
        } catch (IllegalStateException e) {
            System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
            System.exit(1);
        }
    }

    @Override
    public void perform(ArrayList<UUID> selection) {
        if (selection == null)
            return;
        for (UUID notificationID : selection) {
            try {
                dbConnection.markNotificationRead(notificationID, true);
            } catch (SQLException e) {
                System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
                System.exit(1);
            }
        }
    }
}
