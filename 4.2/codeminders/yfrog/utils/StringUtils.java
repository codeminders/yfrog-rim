package codeminders.yfrog.utils;

import java.io.*;
import java.util.*;
import net.rim.device.api.io.*;
import net.rim.device.api.system.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.lib.network.NetworkException;

public class StringUtils {

    private static final String LETTER_DIGIT_CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String NAME_CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ_abcdefghijklmnopqrstuvwxyz";

    public static boolean isNullOrEmpty(String value) {
        return (value == null) || (value.length() == 0);
    }

    public static boolean isTextChar(char c) {
        return (c >= ' ') || (c == Characters.BACKSPACE);
    }

    public static boolean isLetterOrDigitChar(char c) {
        return LETTER_DIGIT_CHARS.indexOf(c) >= 0;
    }

    public static boolean isNameChar(char c) {
        return NAME_CHARS.indexOf(c) >= 0;
    }

    public static String stringOfChar(char c, int length) {
        char[] buf = new char[length];
        Arrays.fill(buf, c);
        return new String(buf);
    }

    public static String[] vectorToStringArr(Vector v) {
        String[] res = new String[v.size()];
        for(int i = v.size() - 1; i >= 0; i--) {
            Object o = v.elementAt(i);
            res[i] = (o != null) ? o.toString() : null;
        }
        return res;
    }

    public static String arrayToMultilineString(Object[] values) {
        if (values == null)
            return "";
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < values.length; i++) {
            if (s.length() > 0) s.append('\n');
            s.append(values[i]);
        }
        return s.toString();
    }

    public static String getExceptionMessage(Throwable e) {
        String msg = (e != null) ? e.getMessage() : null;
        if ((msg != null) && (msg.length() > 0)) {
            if (msg.toLowerCase().indexOf("<html") >= 0)
                msg = null;
            else if (msg.indexOf("<hash") >= 0)
                try { msg = XmlUtils.getNodeValue(msg, new String[] { "hash", "error" } ); }
                catch (Exception ignored) {}
        }
        if (!isNullOrEmpty(msg))
            return msg;
        if (e == null) {
            if (e instanceof NullPointerException)
                return "Null object reference";
            else if (e instanceof ControlledAccessException)
                return "Device security error";
        }
        if (e instanceof NetworkException) {
            msg = ((NetworkException)e).getResponseMessage();
            if (isNullOrEmpty(msg))
                msg = "Network error";
            return msg;
        }
        msg = e.getClass().getName();
        int p = msg.lastIndexOf('.');
        if (p >= 0)
            msg = msg.substring(p + 1);
        return msg;
    }

    public static String replaceString(String value, String substr, String replaceTo) {
        if ((value == null) || isNullOrEmpty(substr))
            return value;
        String s = value;
        StringBuffer res = new StringBuffer();
        int pos;
        while ((pos = s.indexOf(substr)) >= 0) {
            if (pos > 0)
                res.append(s.substring(0, pos));
            if (replaceTo != null)
                res.append(replaceTo);
            s = s.substring(pos + substr.length());
        }
        res.append(s);
        return res.toString();
    }

    public static String trimRight(String s) {
        if (isNullOrEmpty(s))
            return s;
        StringBuffer res = new StringBuffer(s);
        for (int i = res.length() - 1; i >= 0; i--)
            if (res.charAt(i) <= ' ')
                res.setLength(i);
            else
                break;
        return res.toString();
    }

    public static String trimLeft(String s) {
        if (isNullOrEmpty(s))
            return s;
        StringBuffer res = new StringBuffer(s);
        for (int i = res.length(); i > 0; i--)
            if (res.charAt(0) <= ' ')
                res.delete(0, 0);
            else
                break;
        return res.toString();
    }

    public static String formatDouble(double value, int digits, int minDigits) {
        StringBuffer res = new StringBuffer();
        formatDouble(value, digits, minDigits, res);
        return res.toString();
    }

    public static void formatDouble(double value, int digits, int minDigits, StringBuffer res) {
        double absValue = Math.abs(value);
        if (absValue > (double)Long.MAX_VALUE) {
            res.append(Double.toString(value));
            return;
        }

        int digitsMul = 1;
        double roundAdd = 0.5;
        for (int i = digits; i > 0; i--) {
            digitsMul *= 10;
            roundAdd /= 10;
        }

        absValue += roundAdd;
        long intgr = (long)absValue;

        int frac = (int)((absValue - (double)intgr) * (double)digitsMul);
        if (frac < 0)
            frac = 0;
        if (frac > (digitsMul - 1)) {
            intgr++;
            frac = 0;
        }

        if (value < 0.0)
            res.append('-');
        res.append(intgr);
        if (((frac > 0) && (digits > 0)) || (minDigits > 0)) {
            res.append('.');
            int div = digitsMul / 10;
            while (frac > 0) {
                int digit = frac / div;
                res.append(digit);
                frac -= (digit * div);
                div /= 10;
                minDigits--;
            }
            while (minDigits > 0) {
                res.append('0');
                minDigits--;
            }
        }
    }

    // Base64

    public static String base64Encode(String s) {
        try {
            return base64Encode(s.getBytes("UTF-8"));
        }
        catch (Exception ex) {
            return "";
        }
    }

    public static String base64Encode(byte[] data) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        Base64OutputStream os = new Base64OutputStream(bo);
        try {
            os.write(data);
            os.flush();
            os.close();
            bo.close();
            String res = bo.toString();
            return res;
        }
        catch (Exception ex) {
            return null;
        }
    }

    // Split

    public static String[] split(String s, char delimiter) {
        return split(s, new char[] { delimiter });
    }

    public static String[] split(String s, char[] delimiters) {
        Vector res = new Vector();
        // Create list of strings
        int len = s.length();
        StringBuffer buf = null;
        for (int pos = 0; pos < len; pos++) {
            char c = s.charAt(pos);
            // Check delimiters
            boolean isDelimiter = false;
            for (int i = delimiters.length - 1; i >= 0; i--)
                if (delimiters[i] == c) {
                    isDelimiter = true;
                    break;
                }
            if (isDelimiter) {
                // Flush string
                res.addElement((buf != null) ? buf.toString() : "");
                buf = null;
                continue;
            }
            // Add char
            if (buf == null)
                buf = new StringBuffer();
            buf.append(c);
        }
        if (buf != null)
            res.addElement(buf.toString());
        // Return result
        return vectorToStringArr(res);
    }

    // html

    public static String htmlToString(String s) {
        return htmlToString(s, Integer.MAX_VALUE, false);
    }

    public static String htmlToString(String s, int maxLength, boolean endEllipsis) {
        if ((s == null) || (s.length() <= 0))
            return "";
        StringBuffer res = new StringBuffer();
        int resLen = 0; // do not use res.length() bc it's too slow
        int len = s.length();
        for (int p = 0; p < len; p++) {
            if (resLen >= maxLength) {
                if (endEllipsis) {
                    res.setLength(maxLength - 1);
                    res.append((char)0x2026);
                }
                else
                    res.setLength(maxLength);
                break;
            }

            char c = s.charAt(p);
            switch (c) {
                case '<': { // skip tag
                    for (p++; (p < len) && (s.charAt(p) != '>'); p++) { }
                    break;
                }
                case '&': { // special symbol
                    // fetch word
                    StringBuffer sym = new StringBuffer();
                    boolean complete = false;
                    for (p++; p < len; p++) {
                        c = s.charAt(p);
                        if (c == ';') {
                            complete = sym.length() > 0;
                            break;
                        }
                        if (CharacterUtilities.isSpaceChar(c))
                            break;
                        sym.append(c);
                    }
                    // translate word
                    boolean translated = false;
                    if (complete) {
                        translated = true;
                        String word = sym.toString().toLowerCase();
                        if (word.equals("lt"))
                            res.append('<');
                        else if (word.equals("gt"))
                            res.append('>');
                        else if (word.equals("amp"))
                            res.append('&');
                        else if (word.equals("quot"))
                            res.append('"');
                        else if (word.equals("copy"))
                            res.append('c');
                        else
                            translated = false;
                    }
                    // not translated
                    if (translated)
                        resLen++;
                    else {
                        res.append('&');
                        resLen++;
                        res.append(sym.toString());
                        resLen += sym.length();
                        if (complete) {
                            res.append(';');
                            resLen++;
                        }
                    }
                    break;
                }
                default: { // raw character
                    res.append(c);
                    resLen++;
                    break;
                }
            }
        }
        return res.toString();
    }
}

