public class Duration {
    private Integer days;
    private Integer hours;
    private Integer minutes;

    Duration (Integer days, Integer hours, Integer minutes){
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }
//TODO need to implement this method and the rest of the class for getting Duration field in input
    static Duration durationInput (String inputDescription){
        Duration d=new Duration(0,0,0);
      return d;
    }
}
