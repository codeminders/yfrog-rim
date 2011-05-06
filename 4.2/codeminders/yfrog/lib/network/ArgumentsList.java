package codeminders.yfrog.lib.network;

import java.util.Vector;
import java.io.ByteArrayOutputStream;
import net.rim.device.api.util.SimpleSortingVector;
import net.rim.device.api.util.StringUtilities;

import codeminders.yfrog.lib.utils.StringComparatorEx;

public class ArgumentsList {

    private Vector _list = new Vector();

    public ArgumentsList() {
    }

    public void parseFromUrl(String url) {
        if ((url == null) || (url.length() == 0))
            return;
        int start = url.indexOf('?');
        if (start < 0)
            return;
        parse(url.substring(start + 1));
    }

    public void parse(String values) {
        if ((values == null) || (values.length() == 0))
            return;
        int start = 0;
        int valuesLength = values.length();
        while (start < valuesLength) {
            int end = values.indexOf('&', start);
            String keyValue;
            if (end < 0) {
                keyValue = values.substring(start);
                end = valuesLength;
            }
            else {
                keyValue = values.substring(start, end);
                end++;
            }
            start = end;
            if (keyValue.length() == 0)
                continue;
            int pos = keyValue.indexOf('=');
            if ((pos <= 0) || (pos == (keyValue.length() - 1)))
                continue;
            String key = keyValue.substring(0, pos);
            String value = keyValue.substring(pos + 1);
            set(decodeURL(key), decodeURL(value));
        }
    }

    public int size() {
        return _list.size();
    }

    public int indexOfKey(String key) {
        for (int i = size() - 1; i >= 0; i--)
            if (StringUtilities.strEqual(getKey(i), key))
                return i;
        return -1;
    }

    public String getKey(int index) {
        return ((ArgumentItem)_list.elementAt(index)).Key;
    }

    public String get(int index) {
        return ((ArgumentItem)_list.elementAt(index)).Value;
    }

    public String get(String key) {
        int index = indexOfKey(key);
        if (index >= 0)
            return get(index);
        else
            return null;
    }

    public void set(int index, String value) {
        ((ArgumentItem)_list.elementAt(index)).Key = value;
    }

    public void set(String key, String value) {
        int index = indexOfKey(key);
        if (index >= 0)
            set(index, value);
        else
            _list.addElement(new ArgumentItem(key, value));
    }

    // Format

    public String toString() {
        return toString(false, null);
    }

    public String toString(boolean sort, String[] excludeKeys) {
        StringBuffer res = new StringBuffer();
        toString(sort, excludeKeys, res);
        return res.toString();
    }

    public void toString(boolean sort, String[] excludeKeys, StringBuffer res) {
        Vector items;
        if (sort) {
            items = new SimpleSortingVector();
            ((SimpleSortingVector)items).setSortComparator(StringComparatorEx.getInstance(false));
            ((SimpleSortingVector)items).setSort(true);
        }
        else
            items = new Vector();

        int listSize = _list.size();
        for (int i = 0; i < listSize; i++) {
            ArgumentItem arg = (ArgumentItem)_list.elementAt(i);
            if (excludeKeys != null) {
                boolean excluded = false;
                for (int j = excludeKeys.length - 1; j >= 0; j--)
                    if (StringUtilities.strEqual(arg.Key, excludeKeys[j])) {
                        excluded = true;
                        break;
                    }
                if (excluded)
                    continue;
            }
            StringBuffer itemBuf = new StringBuffer();
            encodeURL(arg.Key, itemBuf);
            itemBuf.append('=');
            encodeURL(arg.Value, itemBuf);
            items.addElement(itemBuf.toString());
        }

        int itemsSize = items.size();
        for (int i = 0; i < itemsSize; i++) {
            if (i > 0)
                res.append('&');
            res.append(items.elementAt(i));
        }
    }

    // ArgumentItem

    private static class ArgumentItem {
        public String Key;
        public String Value;
        public ArgumentItem(String key, String value) {
            this.Key = key;
            this.Value = value;
        }
    }

    // URL

    private static char[] hexChars = {
        '0', '1', '2', '3', '4', '5', '6', '7',
        '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    private static boolean isLetterDigit(byte b) {
        return
            ((b >= (byte)'0') && (b <= (byte)'9')) ||
            ((b >= (byte)'A') && (b <= (byte)'Z')) ||
            ((b >= (byte)'a') && (b <= (byte)'z'));
    }

    private static final String sHexChars = "0123456789ABCDEF";

    private static int charsToByte(char c1, char c2) {
        int i;
        int res = 0;
        char _c1 = Character.toUpperCase(c1);
        char _c2 = Character.toUpperCase(c2);
        i = sHexChars.indexOf(_c1);
        if (i < 0)
            return -1;
        res += (i << 4);
        i = sHexChars.indexOf(_c2);
        if (i < 0)
            return -1;
        res += i;
        return res;
    }

    public static String encodeURL(String value) {
        StringBuffer res = new StringBuffer();
        encodeURL(value, res);
        return res.toString();
    }

    public static void encodeURL(String value, StringBuffer res) {
        if ((value == null) || (value.length() == 0))
            return;
        byte data[];
        try { data = value.getBytes("UTF-8"); }
        catch (Exception ex) { data = new byte[0]; }

        int dataLength = data.length;
        for (int i = 0; i < dataLength; i++) {
            if (isLetterDigit(data[i])) {
                res.append((char)data[i]);
                continue;
            }
            switch (data[i]) {
                case (byte)'-':
                case (byte)'_':
                case (byte)'.':
                case (byte)'~':
                    res.append((char)data[i]);
                    continue;
            }
            res.append('%');
            res.append(hexChars[(data[i] >> 4) & 0xF]);
            res.append(hexChars[data[i] & 0xF]);
        }
    }

    public static String decodeURL(String value) {
        if (value == null)
            return null;
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        byte data[] = value.getBytes();

        for (int i = 0; i < data.length; i++) {
            switch (data[i]) {
                case '+':
                    res.write((byte)' ');
                    break;
                case '%':
                    if ((i + 2) < data.length) {
                        int v = charsToByte((char) data[i + 1], (char) data[i + 2]);
                        if (v >= 0) {
                            i += 2;
                            res.write((byte)v);
                        }
                        else
                            res.write((byte)'%');
                    }
                    break;
                default:
                    res.write(data[i]);
            }
        }

        try { return new String(res.toByteArray(), "UTF-8"); }
        catch (Exception ex) { return ""; }
    }
}

