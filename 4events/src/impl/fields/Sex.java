package impl.fields;

import impl.InputManager;

public class Sex {
    private static final Character MALE = 'M';
    private static final Character FEMALE = 'F';

    private char sex;

    Sex (Character sex) {
        this.sex = sex;
    }

    static public Sex sexInput (String inputDescription) {
        Character input;
        do {
            input = InputManager.inputChar(inputDescription + " (" + MALE + "|" + FEMALE + ")");
        } while (! (input.equals(MALE) || input.equals(FEMALE)) );
        return new Sex(input);
    }

    @Override
    public String toString () {
        return Character.toString(sex);
    }
}
