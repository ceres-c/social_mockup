package impl.fields;

import impl.InputManager;

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

    static public Sex sexInput (String inputDescription, boolean inline) {
        Character userInput;
        do {
            userInput = InputManager.inputChar(inputDescription + " (" + MALE + "|" + FEMALE + ")", inline);
            if (userInput == null)
                return null;
        } while (! (userInput.equals(MALE) || userInput.equals(FEMALE)) );
        return new Sex(userInput);
    }

    public char getSex() {
        return sex;
    }

    public boolean equals(Sex other) { return this.sex == other.getSex(); }

    @Override
    public String toString () {
        return Character.toString(sex);
    }
}
