package codeminders.yfrog.twitter;

import java.io.*;
import java.util.*;

import codeminders.yfrog.utils.*;

public class JsonParser {

    public static Object parse(InputStream is) throws IOException {
        JsonParser parser = new JsonParser(is);
        parser.parse();
        return parser.getResult();
    }

    private static class EofException extends IOException {
        public EofException() { super(); }
    }

    private InputStream is;
    private int pushedChar = -1;
    private Vector res = new Vector();

    private JsonParser(InputStream is) {
        this.is = is;
    }

    private Object getResult() {
        switch (res.size()) {
            case 0:
                return null;
            case 1:
                return res.elementAt(0);
            default:
                Object[] resArray = new Object[res.size()];
                res.copyInto(resArray);
                return resArray;
        }
    }

    private char readChar() throws IOException {
        if (pushedChar >= 0) {
            char c = (char)pushedChar;
            pushedChar = -1;
            return c;
        }
        int i = is.read();
        if (i < 0) throw new EofException();
        return (char)i;
    }

    private void skipSpaces() throws IOException {
        char c;
        while ((c = readChar()) <= ' ') { }
        pushedChar = c;
    }

    private char read4DigitsChar() throws IOException {
        char[] res = new char[4];
        for (int i = 0; i < 4; i++)
            res[i] = readChar();
        return (char)Integer.parseInt(new String(res), 16);
    }

    private void parse() throws IOException {
        res.removeAllElements();
        while (true)
            try {
                Object o = readValue(true);
                if (o != null)
                    res.addElement(o);
            }
            catch (EofException ex) {
                break;
            }
    }

    private Object readValue(boolean objectsOnly) throws IOException {
        skipSpaces();
        char c = readChar();
        if (c == '[')
            return readArray();
        else if (c == '{')
            return readObject();
        if (objectsOnly)
            return null;
        if (c == '"')
            return readQuotedString();
        String s = readNonQuotedString(c);
        if ("true".equals(s))
            return new Boolean(true);
        else if ("false".equals(s))
            return new Boolean(false);
        else if ("null".equals(s))
            return null;
        /*
        else if (
            (s.indexOf('.') >= 0)
            || (s.indexOf('E') >= 0)
            || (s.indexOf('e') >= 0)
        )
            return new Double(Double.parseDouble(s));
        else
            return new Long(Long.parseLong(s));
        */
        else
            return s;
    }

    private String readQuotedString() throws IOException {
        StringBuffer res = new StringBuffer();
        while (true) {
            char c = readChar();
            if (c == '"')
                break;
            else if (c == '\\') {
                c = readChar();
                switch (c) {
                    case '"': res.append('"'); break;
                    case '\\': res.append('\\'); break;
                    case '/': res.append('/'); break;
                    case 'b': res.append('\b'); break;
                    case 'f': res.append('\f'); break;
                    case 'n': res.append('\n'); break;
                    case 'r': res.append('\r'); break;
                    case 't': res.append('\t'); break;
                    case 'u': res.append(read4DigitsChar()); break;
                }
            }
            else
                res.append(c);
        }
        return res.toString();
    }

    private static final String controlChars = "[]{}:,\"";
    private String readNonQuotedString(char firstChar) throws IOException {
        StringBuffer res = new StringBuffer();
        res.append(firstChar);
        int c;
        while ((c = is.read()) >= 0) {
            if (controlChars.indexOf(c) >= 0) {
                pushedChar = c;
                break;
            }
            res.append((char)c);
        }
        return res.toString();
    }

    private Object readArray() throws IOException {
        Vector res = new Vector();
        while (true) {
            skipSpaces();
            char c = readChar();
            if (c == ']')
                break;
            else if (c == '{')
                res.addElement(readObject());
        }
        Object[] resArr = new Object[res.size()];
        res.copyInto(resArr);
        return resArr;
    }

    private Object readObject() throws IOException {
        Hashtable res = new Hashtable();
        while (true) {
            String name;
            skipSpaces();
            char c = readChar();
            if (c == '}')
                break;
            else if (c == ',')
                continue;
            else if (c == '"')
                name = readQuotedString();
            else
                name = readNonQuotedString(c);
            Object value = null;
            skipSpaces();
            c = readChar();
            if (c != ':') {
                pushedChar = c;
                continue;
            }
            value = readValue(false);
            if ((value == null) || StringUtils.isNullOrEmpty(name))
                continue;
            res.put(name, value);
        }
        return res;
    }
}

