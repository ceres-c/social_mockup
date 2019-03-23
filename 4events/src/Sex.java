public class Sex {
    // TODO move this class to own package with other needed "Field classes"
    private static final Character MALE = 'M';
    private static final Character FEMALE = 'F';

    private char sex;

    Sex (Character sex) {
        this.sex = sex;
    }

    static Sex sexInput (String inputDescription) {
        Character input;
        do {
            input = InputManager.inputChar(inputDescription + " (" + MALE + "|" + FEMALE + ")");
        } while (! (input.equals(MALE) || input.equals(FEMALE)) );
        return new Sex(input);
    }
}
