package impl.fields;

import impl.InputManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyDuration {
    private Integer minutes;

    MyDuration(Integer minutes){
        this.minutes = minutes;
    }

    public int sizeOf() { // ;)
        return minutes;
    }

    static public MyDuration durationInput (String inputDescription){
        String input;
        Boolean validInput = false;
        Integer minutes = 0;
        do {
            input = InputManager.inputString(inputDescription + " (#D#H#M)");
            if (input != null) {
                Pattern r = Pattern.compile("(\\d*D)?(\\d*H)?(\\d*M)?"); // Accepts all strings like 10D11H12M, but also like "10D", "11H" or "12M"
                Matcher matcher = r.matcher(input);
                if (matcher.matches()) {
                    try {
                        validInput = true;
                        int[] array = new int[3];
                        int j = 0;
                        for (int i = 1; i <= 3; i++) {
                            if (matcher.group(i) != null) {
                                array[j] = Integer.parseInt(matcher.group(i).replaceAll("[DHM]", "")); // Removes time unit identifier chars
                            } else array[j] = 0;
                            j++;
                        }
                        minutes = (array[0] * 1440) + (array[1] * 60) + array[2];
                    } catch (NumberFormatException e) {
                        // This should never happen (TM)
                        System.out.println("ALERT: Illegal user input: " + input);
                        e.printStackTrace();
                    }
                } else validInput = false;
            } else
                return null;
        } while(!validInput && input != null);
        return new MyDuration(minutes);
    }
}
