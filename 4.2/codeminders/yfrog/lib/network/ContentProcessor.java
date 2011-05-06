package codeminders.yfrog.lib.network;

import java.util.Hashtable;
import java.io.InputStream;

public interface ContentProcessor {
    Object process(
        NetworkRequest request,
        int responseCode, Hashtable responseHeaders, InputStream responseData
    ) throws Exception;
}

