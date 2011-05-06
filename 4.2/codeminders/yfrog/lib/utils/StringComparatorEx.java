package codeminders.yfrog.lib.utils;

import net.rim.device.api.util.*;

public class StringComparatorEx implements Comparator {

    private boolean ignoreCase;

    private StringComparatorEx(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    public static final Comparator getInstance(boolean ignoreCase) {
        return new StringComparatorEx(ignoreCase);
    }

    public int compare(Object o1, Object o2) {
        String s1 = o1.toString();
        if (s1 == null) s1 = "";
        String s2 = o2.toString();
        if (s2 == null) s2 = "";
        if (ignoreCase) {
            int res = StringUtilities.compareToIgnoreCase(s1, s2);
            if (res != 0) return res;
        }
        return s1.compareTo(s2);
    }
}

