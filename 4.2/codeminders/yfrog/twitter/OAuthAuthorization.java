package codeminders.yfrog.twitter;

import java.util.Date;
import java.util.Random;
import java.io.IOException;
import javax.microedition.io.HttpConnection;
import net.rim.device.api.crypto.*;

import codeminders.yfrog.lib.network.ArgumentsList;
import codeminders.yfrog.lib.network.HttpMethod;
import codeminders.yfrog.utils.StringUtils;

public class OAuthAuthorization implements Authorization {

    private String _consumerKey = null;
    private String _consumerSecret = null;
    private String _token = null;
    private String _tokenSecret = null;

    public OAuthAuthorization(String consumerKey, String consumerSecret) {
        this(consumerKey, consumerSecret, null, null);
    }

    public OAuthAuthorization(String consumerKey, String consumerSecret, String token, String tokenSecret) {
        _consumerKey = consumerKey;
        _consumerSecret = consumerSecret;
        _token = token;
        _tokenSecret = tokenSecret;
    }

    public void applyToArgs(int httpMethod, String url, ArgumentsList args) {
        if (!StringUtils.isNullOrEmpty(_consumerKey))
            args.set("oauth_consumer_key", _consumerKey);
        if (!StringUtils.isNullOrEmpty(_token))
            args.set("oauth_token", _token);

        args.set("oauth_signature_method", "HMAC-SHA1");
        args.set("oauth_timestamp", generateTimestamp());
        args.set("oauth_nonce", generateNonce());
        args.set("oauth_version", "1.0");

        args.set("oauth_signature", generateSignature(httpMethod, url, args));
    }

    public void applyToRequest(HttpConnection request) throws IOException {
    }

    private String generateTimestamp() {
        return Long.toString(new Date().getTime() / 1000L);
    }

    private String generateNonce() {
        return Long.toString(new Random().nextLong());
    }

    private String generateSignature(int httpMethod, String url, ArgumentsList args) {
        StringBuffer baseString = new StringBuffer();
        baseString.append(HttpMethod.toString(httpMethod));
        baseString.append('&');
        ArgumentsList.encodeURL(url, baseString);
        baseString.append('&');
        ArgumentsList.encodeURL(
            args.toString(true, new String[] { "oauth_signature" }),
            baseString);

        StringBuffer keyString = new StringBuffer();
        ArgumentsList.encodeURL(_consumerSecret, keyString);
        keyString.append('&');
        ArgumentsList.encodeURL(_tokenSecret, keyString);

        try {
            HMACKey key = new HMACKey(keyString.toString().getBytes("US-ASCII"));
            MAC mac = MACFactory.getInstance("HMAC/SHA1", key);

            byte[] data = baseString.toString().getBytes("US-ASCII");
            mac.update(data, 0, data.length);
            byte[] hashBytes = mac.getMAC();

            return StringUtils.base64Encode(hashBytes);
        }
        catch (Exception ex) {
            return "";
        }
    }
}

