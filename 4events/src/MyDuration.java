import java.util.regex.Matcher;
import java.util.regex.Pattern;
// TODO move this class to own package with other needed "Field classes"

public class MyDuration {
    private Integer minutes;

    MyDuration(Integer minutes){
        this.minutes = minutes;
    }

    static MyDuration durationInput (String inputDescription){
        String input;
        Boolean validInput = false;
        Integer minutes = 0;
        do {
            input = InputManager.inputString(inputDescription+"(#D#H#M)");
            Pattern r = Pattern.compile("(?:(\\d)*D)?(?:(\\d)*H)?(?:(\\d)*M)?");
            Matcher matcher = r.matcher(input);
            if(matcher.matches()){
                try {
                    validInput=true;
                    int[] array=new int[3];
                    int j=0;
                    for(int i=1;i<=3;i++) {
                        if (matcher.group(i) != null){
                            array[j] = Integer.parseInt(matcher.group(i));
                        }else array[j]=0;
                        j++;
                    }
                    minutes=(array[0]*1440)+(array[1]*60)+array[2];
                }catch (NumberFormatException e){
                    // This should never happen (TM)
                }
            }else validInput = false;
        }while (!validInput);
        return new MyDuration(minutes);
    }
}
