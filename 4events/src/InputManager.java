import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Scanner;
import java.util.regex.Pattern;

public class InputManager {

    private static Scanner in = new Scanner(System.in);

    /**
     * Given a Class object, this method chooses the right kind of input needed
     * WARNING: return is null if the user did not write anything!
     * // TODO probably we should move to input methods in relevant classes to allow logical sanitization of user input
     * @param inputDescription A descriptive text to tell the user which input is required
     * @param type java.lang.Class type of needed input
     * @return A generic T which contains user input and HAS to be cast to the right type - WARNING: can be null!
     */
    public static <T> T genericInput(String inputDescription, Class type) {
        if (type.equals(Integer.class)) {
            return (T) inputInteger(inputDescription);
        } else if (type.equals(Double.class)) {
            return (T) inputDouble(inputDescription);
        } else if (type.equals(String.class)) {
            return (T) inputString(inputDescription);
        } else if (type.equals(Character.class)) {
            return (T) inputChar(inputDescription);
        } else if (type.equals(Calendar.class)) {
            System.out.println("Save the date");
            return (T) Calendar.getInstance(); // TODO NEED REAL INPUT FUNCTION
        } else if (type.equals(Sex.class)) {
            return (T) Sex.sexInput(inputDescription);
        } else {
            System.out.println("WTF? " + type);
            return (T) type; // TODO REMOVE THIS
        }
    }

    public static String inputString(String inputDescription) {
        System.out.print(inputDescription + ": ");
        String input = in.nextLine().trim();
        if (input.length() > 0)
            return input;
        else
            return null; // CHECK FOR NULL-OBJECT!
    }

    public static Double inputDouble(String inputDescription) {
        boolean validInput;
        boolean checkPattern;
        Double inputNumber = 0.00; // Just to shut the compiler up, this variable WILL be initialized once we return
        Pattern pattern = Pattern.compile("^\\d+(,\\d{3})*(\\.\\d{1,2})?$");
        do {
            System.out.print(inputDescription + ": ");
            String input = in.nextLine().trim();
            checkPattern = pattern.matcher(input).matches();
            if (input.length() == 0)
                return null; // CHECK FOR NULL-OBJECT!
            try {
                validInput = true;
                inputNumber = Double.parseDouble(input);
            } catch (NumberFormatException exception) {
                validInput = false;
                System.out.println("ALERT: Number expected!");
            }
            if(!checkPattern){
                System.out.print("ALERT: Insert a number in pattern (#.##)!\n");
            }
        } while (!validInput || !checkPattern);
        return inputNumber;
    }

    public static Character inputChar(String inputDescription) {
        System.out.print(inputDescription + ": ");
        String input = in.nextLine().trim();
        if (input.length() > 0)
            return input.charAt(0);
        else
            return null; // CHECK FOR NULL-OBJECT!
    }

    public static Integer inputInteger(String inputDescription) {
        boolean validInput;
        Integer inputNumber = 0; // Just to shut the compiler up, this variable WILL be initialized once we return
        do {
            System.out.print(inputDescription + ": ");
            String input = in.nextLine().trim();
            if (input.length() == 0)
                return null; // CHECK FOR NULL-OBJECT!
            try {
                validInput = true;
                inputNumber = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                validInput = false;
                System.out.println("ALERT: Integer  number expected!");
            }
        } while (!validInput);
        return inputNumber;
    }
}
