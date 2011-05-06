package codeminders.yfrog.twitter;

import java.io.IOException;
import javax.microedition.io.HttpConnection;
import net.rim.device.api.io.http.HttpHeaders;

import codeminders.yfrog.lib.network.ArgumentsList;
import codeminders.yfrog.utils.StringUtils;

public class BasicAuthorization implements Authorization {

    private String _username;
    private String _password;

    public BasicAuthorization(String username, String password) {
        _username = username;
        _password = password;
    }

    public void applyToArgs(int httpMethod, String url, ArgumentsList args){
    }

    public void applyToRequest(HttpConnection request) throws IOException {
        if ((_username == null) || (_username.length() <= 0))
            return;
        request.setRequestProperty(HttpHeaders.HEADER_AUTHORIZATION,
            "Basic " + StringUtils.base64Encode(
                _username + ':' + ((_password != null) ? _password : "")));
    }
}

