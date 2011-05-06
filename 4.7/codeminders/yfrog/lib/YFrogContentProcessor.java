/*
 * YFrogContentProcessor.java
 *
 */

package codeminders.yfrog.lib;

import java.util.Hashtable;
import java.io.InputStream;
import net.rim.device.api.xml.parsers.SAXParserFactory;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import codeminders.yfrog.lib.utils.*;
import codeminders.yfrog.lib.YFrogXmlHandler;
import codeminders.yfrog.lib.network.*;

/**
 *
 */
public class YFrogContentProcessor implements ContentProcessor  {
    public YFrogContentProcessor() {
    }
    public Object process(
        NetworkRequest request,
        int responseCode, Hashtable responseHeaders, InputStream responseData
    ) throws Exception {
            YFrogXmlHandler handler = new YFrogXmlHandler();
            SAXParserFactory.newInstance().newSAXParser().parse(responseData, handler);
            return handler.getResult();
    }

}
