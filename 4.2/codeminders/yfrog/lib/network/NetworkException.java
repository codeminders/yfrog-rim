package codeminders.yfrog.lib.network;

import java.io.IOException;

public class NetworkException extends IOException {

    private int _responseCode = -1;
    private String _responseMessage = null;

    public NetworkException() {
        super();
    }

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(String message, int responseCode, String responseMessage) {
        super(message);
        _responseCode = responseCode;
        _responseMessage = responseMessage;
    }

    public int getResponseCode() {
        return _responseCode;
    }

    public String getResponseMessage() {
        return _responseMessage;
    }
}

