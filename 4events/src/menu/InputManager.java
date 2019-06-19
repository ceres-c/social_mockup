package menu;

import model.fields.OptionalCost;
import model.fields.Sex;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All the methods in this class return null if the user inputs a null string
 */
public class InputManager {

    private static Scanner in = new Scanner(System.in);

    /**
     * Given a Class object, this method chooses the right kind of input needed
     * WARNING: return is null if the user did not write anything!
     * @implNote If this method has to be changed, probably also Connector.genericDBGetter should be changed accordingly
     * @param inputDescription A descriptive text to tell the user which input is required
     * @param type java.lang.Class type of needed input
     * @param inline if true inputDescription is shown inline, otherwise on next line prepended with an ascii arrow
     * @return A generic T which contains user input and HAS to be cast to the right type - WARNING: can be null!
     * @throws IllegalArgumentException If this method is fed an unknown class type
     */
    static <T> T genericInput(String inputDescription, Class type, boolean inline) throws IllegalArgumentException {
        if (type.equals(Integer.class)) {
            return (T) inputInteger(inputDescription, inline);
        } else if (type.equals(Double.class)) {
            return (T) inputDouble(inputDescription, inline);
        } else if (type.equals(String.class)) {
            return (T) inputString(inputDescription, inline);
        } else if (type.equals(LocalDateTime.class)) {
            return (T) inputDateTime(inputDescription, inline);
        } else if (type.equals(Duration.class)) {
            return (T) inputDuration(inputDescription, inline);
        } else if (type.equals(Sex.class)) {
            return (T) Sex.sexInput(inputDescription, inline);
        } else if (type.equals(OptionalCost.class)) {
            return (T) OptionalCost.optionalCostInput(inputDescription, inline);
        } else {
            throw new IllegalArgumentException("ALERT: Unexpected input type: " + type);
        }
    }

    static String inputString(String inputDescription, boolean inline) {
        if (inline)
            System.out.print(inputDescription + ": ");
        else
            System.out.print(inputDescription + "\n--> ");
        String input = in.nextLine().trim();
        if (input.length() > 0)
            return input;
        else
            return null; // CHECK FOR NULL-OBJECT!
    }

    static char[] inputPassword(String inputDescription) {
        char[] password;
        java.io.Console console = System.console();
        if (console != null) { // Probably running in a standard terminal
            password = console.readPassword(inputDescription + ": ");
            if (password.length == 0)
                password = null;
        } else { // Probably running inside an IDE
            String passwordString;
            System.out.println("ALERT! Password will be displayed in plaintext since this program seems to be running in a non-compliant shell");
            while ( (passwordString = InputManager.inputString("Password", true)) == null );
            password = passwordString.toCharArray(); // This is defying the scope of using a char array, but given we'are already writing in plain text I guess it's not the worst thing here
        }
        return password;
    }

    static Double inputDouble(String inputDescription, boolean inline) {
        boolean validInput;
        boolean checkPattern;
        double inputNumber = 0.00; // Just to shut the compiler up, this variable WILL be initialized once we return
        Pattern pattern = Pattern.compile("^\\d{1,3}(,\\d{3})*(\\.\\d{1,2})?$");
        do {
            if (inline)
                System.out.print(inputDescription + " (#.##): ");
            else
                System.out.print(inputDescription + " (#.##):\n--> ");
            String input = in.nextLine().trim();
            checkPattern = pattern.matcher(input).matches();
            input = input.replaceAll(",", "");
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

    public static Character inputChar(String inputDescription, boolean inline) {
        if (inline)
            System.out.print(inputDescription + ": ");
        else
            System.out.print(inputDescription + "\n--> ");
        String input = in.nextLine().trim();
        if (input.length() > 0)
            return input.charAt(0);
        else
            return null; // CHECK FOR NULL-OBJECT!
    }

    public static Integer inputInteger(String inputDescription, boolean inline) {
        boolean validInput;
        Integer inputNumber = 0; // Just to shut the compiler up, this variable WILL be initialized once we return
        do {
            if (inline)
                System.out.print(inputDescription + ": ");
            else
                System.out.print(inputDescription + "\n--> ");
            String input = in.nextLine().trim();
            if (input.length() == 0)
                return null; // CHECK FOR NULL-OBJECT!
            try {
                validInput = true;
                inputNumber = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                validInput = false;
                System.out.println("ALERT: Integer number expected!");
            }
        } while (!validInput);
        return inputNumber;
    }

    static LocalDateTime inputDateTime(String inputDescription, boolean inline) {
        boolean validInput = false; // Just to shut the compiler up.
        LocalDateTime date = null;  // These two variables WILL be initialized once we hit return
        do {
            if (inline)
                System.out.print(inputDescription + " (DD/MM/YYYY) or (DD/MM/YYYY HH:MM): ");
            else
                System.out.print(inputDescription + " (DD/MM/YYYY) or (DD/MM/YYYY HH:MM):\n--> ");
            String input = in.nextLine();
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            if (input.length() == 0)
                return null;
            try {
                validInput = true;
                date = LocalDate.parse(input, dateFormat).atStartOfDay();
            } catch (DateTimeParseException e) { // The user could have used hours and minutes too
                try { // YAY! Nested try-catches
                    date = LocalDateTime.parse(input, dateTimeFormat);
                } catch (DateTimeParseException ex) { // Well, the user screwed up
                    validInput = false;
                    System.out.println("ALERT: wrong pattern");
                }
            }
        } while (!validInput);
        return date ;
    }

    static Duration inputDuration(String inputDescription, boolean inline) {
        boolean validInput = false;
        Duration duration = null;
        StringBuilder durationBuilder = null;

        do {
            if (inline)
                System.out.print(inputDescription + " (#D#H#M): ");
            else
                System.out.print(inputDescription + " (#D#H#M):\n--> ");
            String input = in.nextLine();
            if (input.length() == 0)
                return null;
            Pattern r = Pattern.compile("(\\d*D)?(\\d*H)?(\\d*M)?"); // Accepts all strings like 10D11H12M, but also only "10D", "11H" or "12M"
            Matcher matcher = r.matcher(input);
            if (matcher.matches()) { // We have to build a string that can be accepted by Duration.parse such as "P2DT3H4M"
                validInput = true;
                durationBuilder = new StringBuilder();
                durationBuilder.append("P");
                if (matcher.group(1) != null) durationBuilder.append(matcher.group(1));
                if (matcher.group(2) != null || matcher.group(3) != null) durationBuilder.append("T");
                // According to ISO standards Days and Hours/Minutes must be separated with a T, but it's not needed if we only have days
                if (matcher.group(2) != null) durationBuilder.append(matcher.group(2));
                if (matcher.group(3) != null) durationBuilder.append(matcher.group(3));
            }
            if (validInput) {
                try {
                    duration = Duration.parse(durationBuilder);
                } catch (DateTimeParseException e) {
                    // This should never happen (TM)
                    validInput = false;
                    System.out.println("ALERT: Illegal user input: " + input);
                    e.printStackTrace();
                }
            }
        } while (!validInput);
        return duration ;
    }

    static ArrayList<Integer> inputNumberSequence(String inputDescription, boolean inline) {
        ArrayList<Integer> inputNumbers = new ArrayList<>();

        if (inline)
            System.out.print(inputDescription + ": ");
        else
            System.out.print(inputDescription + "\n--> ");

        String input = in.nextLine().trim();
        if (input.length() == 0)
            return null; // CHECK FOR NULL-OBJECT!
        String[] stringNumbers = input.split("\\s*,\\s*");
        ArrayList<Integer> intNumbers = new ArrayList<>();
        int i;
        try {
            for (String string : stringNumbers)
                intNumbers.add(Integer.parseInt(string));
        } catch (NumberFormatException e) {
            System.out.println("ALERT: Number expected!");
        }

        return intNumbers;
    }
}
