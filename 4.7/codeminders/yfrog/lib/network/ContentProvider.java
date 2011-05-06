package codeminders.yfrog.lib.network;

import java.io.IOException;
import java.io.OutputStream;

public interface ContentProvider {
    String getContentType();
    int getContentLength();
    String getContentRange();
    void writeContent(OutputStream os) throws IOException;
}

