package it.unibs.ing.se.view;

import it.unibs.ing.se.DMO.JsonTranslator;

public class LoginSignupView implements PrintableInterface<Integer> {
    JsonTranslator menuTranslation;

    // TODO method description
    public LoginSignupView() {
        this.menuTranslation = new JsonTranslator(JsonTranslator.MENU_JSON_PATH);
    }

    @Override
    public void print() {
        System.out.print(menuTranslation.getTranslation("loginOrSignup"));
    }

    @Override
    public Integer parseInput() {
        Integer userInput = 0;
        while (userInput != 1 && userInput != 2) {
            userInput = InputManager.inputInteger("", true);
            userInput = (userInput == null ? 0 : userInput); // NullObjectExceptions are FUN
        }
        return userInput;
    }
}
