import java.util.GregorianCalendar;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Created by gokul on 11/30/15.
 */
public class GenerateRandomStuff {
    public static String getDOB(){
        GregorianCalendar gc = new GregorianCalendar();
        int year = randBetween(1957, 1997);
        gc.set(gc.YEAR, year);
        int dayOfYear = randBetween(1, gc.getActualMaximum(gc.DAY_OF_YEAR));
        gc.set(gc.DAY_OF_YEAR, dayOfYear);
        return(gc.get(gc.YEAR) + "-" + (gc.get(gc.MONTH) + 1) + "-" + gc.get(gc.DAY_OF_MONTH));
    }

    public static String getEmail() {
        return(RandomStringUtils.randomAlphanumeric(10)+"@facebook.com");
    }
    public static int randBetween(int start, int end) {
        return start + (int)Math.round(Math.random() * (end - start));
    }
    public static String getGender(int x){
        if (x == 0)
            return "Male";
        return "Female";
    }
    public static String getName() {
        return(RandomStringUtils.randomAlphabetic(10));
    }
    public static String getString(int length) {
        return(RandomStringUtils.randomAlphanumeric(length));
    }

}
