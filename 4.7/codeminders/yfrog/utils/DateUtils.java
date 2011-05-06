package codeminders.yfrog.utils;

import java.util.*;

public class DateUtils {

    public static final long FULL_DAY = 24 * 3600 * 1000;

    public static int getTZOffset() {
        return getTZOffset(TimeZone.getDefault());
    }

    public static int getTZOffset(TimeZone tz) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int res = tz.getOffset(
            1,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.DAY_OF_WEEK),
            0);
        return res;
    }

    public static int getDaysToToday(long date) {
        long tzOffset = getTZOffset();
        long dayNow = (new Date().getTime() + tzOffset) / FULL_DAY;
        long dayDate = (date + tzOffset) / FULL_DAY;
        return (int)(dayNow - dayDate);
    }

    public static boolean isTodayYear(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int yearNow = cal.get(Calendar.YEAR);
        cal.setTime(new Date(date));
        int yearDate = cal.get(Calendar.YEAR);
        return (yearNow == yearDate);
    }
}

