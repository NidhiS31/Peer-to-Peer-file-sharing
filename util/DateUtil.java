package util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtil {
    public static String getDateAndTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(calendar.getTime());
    }
}