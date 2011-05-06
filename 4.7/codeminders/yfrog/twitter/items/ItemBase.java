package codeminders.yfrog.twitter.items;

import java.util.Calendar;
import java.util.Date;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.DateEx;
import codeminders.yfrog.utils.DateUtils;
import codeminders.yfrog.utils.StringUtils;

public class ItemBase implements Persistable {
    protected ItemBase() { }

    public void setField(String name, String value) { }
    public ItemBase newChildren(String name) { return null; }

    public static String parseText(String value) {
        return StringUtils.htmlToString(value);
    }

    public static long parseDate(String value) {
        String[] words = StringUtils.split(value.toLowerCase().trim(), new char[] { ' ', ':' });
        if (words.length != 8)
            return new Date().getTime();
        int year, month, day, hour, minute, second, offset;

        if ("jan".equals(words[1])) month = Calendar.JANUARY;
        else if ("feb".equals(words[1])) month = Calendar.FEBRUARY;
        else if ("mar".equals(words[1])) month = Calendar.MARCH;
        else if ("apr".equals(words[1])) month = Calendar.APRIL;
        else if ("may".equals(words[1])) month = Calendar.MAY;
        else if ("jun".equals(words[1])) month = Calendar.JUNE;
        else if ("jul".equals(words[1])) month = Calendar.JULY;
        else if ("aug".equals(words[1])) month = Calendar.AUGUST;
        else if ("sep".equals(words[1])) month = Calendar.SEPTEMBER;
        else if ("oct".equals(words[1])) month = Calendar.OCTOBER;
        else if ("nov".equals(words[1])) month = Calendar.NOVEMBER;
        else if ("dec".equals(words[1])) month = Calendar.DECEMBER;
        else return new Date().getTime();

        try { year = Integer.parseInt(words[7]); } catch (Exception ex) { return new Date().getTime(); }
        try { day = Integer.parseInt(words[2]); } catch (Exception ex) { return new Date().getTime(); }
        try { hour = Integer.parseInt(words[3]); } catch (Exception ex) { return new Date().getTime(); }
        try { minute = Integer.parseInt(words[4]); } catch (Exception ex) { return new Date().getTime(); }
        try { second = Integer.parseInt(words[5]); } catch (Exception ex) { return new Date().getTime(); }

        try { offset = Integer.parseInt(
            (words[6].charAt(0) == '+') ? words[6].substring(1) : words[6]
        ); } catch (Exception ex) { return new Date().getTime(); }
        offset = (offset / 100 * 60) + (offset % 100);

        DateEx date = new DateEx(0);
        date.set(year, month, day, hour, minute, second, 0);
        long res = date.getTime();
        res += offset * 60 * 1000;
        return res;
    }
}

