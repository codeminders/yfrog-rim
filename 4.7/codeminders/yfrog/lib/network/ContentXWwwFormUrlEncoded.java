package codeminders.yfrog.lib.network;

import java.io.IOException;
import java.io.OutputStream;
import net.rim.device.api.io.http.HttpProtocolConstants;

public class ContentXWwwFormUrlEncoded implements ContentProvider {

    private byte[] _data;

    public ContentXWwwFormUrlEncoded(ArgumentsList args) {
        String argsData = (args != null) ? args.toString() : "";
        try { _data = argsData.getBytes("UTF-8"); }
        catch (Exception ex) { _data = new byte[0]; }
    }

    public String getContentType() {
        return HttpProtocolConstants.CONTENT_TYPE_APPLICATION_X_WWW_FORM_URLENCODED;
    }

    public int getContentLength() {
        return _data.length;
    }

    public String getContentRange() {
        return null;
    }

    public void writeContent(OutputStream os) throws IOException {
        os.write(_data);
    }
}

