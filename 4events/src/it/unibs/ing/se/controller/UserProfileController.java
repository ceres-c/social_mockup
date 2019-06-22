package it.unibs.ing.se.controller;

import it.unibs.ing.se.DMO.Connector;
import it.unibs.ing.se.model.User;
import it.unibs.ing.se.model.UserCore;

import java.sql.SQLException;
import java.util.UUID;

public class UserProfileController implements ControllerInterface<UserCore>  {
    private User currentUser;
    Connector dbConnection;

    UserProfileController(UUID currentUserID) {
        dbConnection = Connector.getInstance();
        try {
            currentUser = dbConnection.getUser(currentUserID);
        } catch (SQLException e) {
            System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
            System.exit(1);
        }
    }

    @Override
    public void perform(UserCore selection) {
        if (!selection.equals(currentUser)) { // Then the user changed some details
            User updatedUser = new User(
                    selection.getUsername(),
                    selection.getHashedPassword(),
                    currentUser.getUserID(),
                    selection.getGender(),
                    selection.getAge(),
                    selection.getFavoriteCategories()
            );
            try {
                dbConnection.updateUser(updatedUser);
            } catch (SQLException e) {
                System.err.println("FATAL: Impossible to connect to SQL database. Contact your sysadmin");
                System.exit(1);
            }
        }
    }
}
