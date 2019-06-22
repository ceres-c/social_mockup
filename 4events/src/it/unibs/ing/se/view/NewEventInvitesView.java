package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.JsonTranslator;

public class NewEventInvitesView implements PrintableInterface<Boolean> {
    protected JsonTranslator menuTranslation;

    public NewEventInvitesView() {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
    }

    @Override
    public void print() {} // A man's gotta do what a man's gotta do

    @Override
    public Boolean parseInput() {
        Character userInput;
        do {
            userInput = InputManager.inputChar(menuTranslation.getTranslation("sendInvite"), true);
            if (userInput != null && userInput != 'S' && userInput != 'N')
                userInput = null;
        } while (userInput == null);
        return userInput == 'S';
    }
}
