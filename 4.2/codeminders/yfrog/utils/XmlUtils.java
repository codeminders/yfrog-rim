package codeminders.yfrog.utils;

import java.io.*;
import net.rim.device.api.util.*;
import net.rim.device.api.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class XmlUtils {

    public static class SAXStopException extends SAXException {
        public SAXStopException() { super(""); }
    }

    /******************/
    /* getNodeValue() */
    /******************/

    private static class DefaultHandlerGetNodeValue extends DefaultHandler {

        private String[] path;
        private String[] pathFound;
        private int pathSize;

        private int level = -1;
        private StringBuffer value = null;

        public DefaultHandlerGetNodeValue(String[] path) {
            this.pathSize = path.length;
            this.path = path;
            this.pathFound = new String[pathSize];
            for (int i = pathSize - 1; i >= 0; i--)
                pathFound[i] = null;
        }

        public String getValue() {
            return (value != null) ? value.toString() : null;
        }

        // DefaultHandler

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            level++;
            if (level < pathSize)
                pathFound[level] = localName;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (value != null)
                throw new SAXStopException();
            level--;
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
            if (level != (pathSize - 1))
                return;
            for (int i = pathSize - 1; i >= 0; i--)
                if (!StringUtilities.strEqual(path[i], pathFound[i]))
                    return;
            if (value == null)
                value = new StringBuffer();
            value.append(ch, start, length);
        }
    }

    public static String getNodeValue(String xml, String[] path) throws Exception {
        DefaultHandlerGetNodeValue handler = new DefaultHandlerGetNodeValue(path);

        ByteArrayInputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(is, handler);
        }
        catch (SAXStopException ex) { }
        finally { try { is.close(); } catch (Exception ignored) {} }

        String res = handler.getValue();
        if (res == null)
            throw new Exception("Node not found");
        return res;
    }
}

