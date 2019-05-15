package impl.fields;

import impl.InputManager;

import java.util.stream.Stream;

public class Sex {
    private static final Character MALE = 'M';
    private static final Character FEMALE = 'F';

    private char sex;

    public Sex (Character sex) {
        this.sex = sex;
    }

    public Sex (String sex) {
        this.sex = sex.charAt(0);
    }

    static public Sex sexInput (String inputDescription) {
        Character input;
        do {
            input = InputManager.inputChar(inputDescription + " (" + MALE + "|" + FEMALE + ")");
            if (input == null)
                return null;
        } while (! (input.equals(MALE) || input.equals(FEMALE)) );
        return new Sex(input);
    }

    public char getSex() {
        return sex;
    }

    @Override
    public String toString () {
        return Character.toString(sex);
    }
}
