package controller;

import menu.commands.MainCommand;
import model.User;

public class MainMenuController implements ControllerInterface<MainCommand> {
    private User currentUser;

    @Override
    public void perform(MainCommand selection) {
        switch (selection) {
            case DASHBOARD:
                break;
            case LOGOUT:
        }
    }
}
