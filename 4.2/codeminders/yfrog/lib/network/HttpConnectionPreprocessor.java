package codeminders.yfrog.lib.network;

import java.io.IOException;
import javax.microedition.io.HttpConnection;

public interface HttpConnectionPreprocessor {
    void applyToRequest(HttpConnection request) throws IOException;
}

