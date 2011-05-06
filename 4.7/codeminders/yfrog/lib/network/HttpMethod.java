package codeminders.yfrog.lib.network;

import net.rim.device.api.io.http.HttpProtocolConstants;

public class HttpMethod {

    public static final int GET = 0;
    public static final int POST = 1;
    public static final int PUT = 2;

    public static String toString(int method) {
        switch (method) {
            case GET:
                return HttpProtocolConstants.HTTP_METHOD_GET;
            case POST:
                return HttpProtocolConstants.HTTP_METHOD_POST;
            case PUT:
                return HttpProtocolConstants.HTTP_METHOD_PUT;
            default:
                return "";
        }
    }
}

