package codeminders.yfrog.app.forms;

import java.io.*;
import javax.microedition.io.*;
import net.rim.device.api.browser.plugin.*;
import net.rim.device.api.browser.field.*;
import net.rim.device.api.io.http.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.network.*;

public class WebBrowserScreen extends ScreenBase {

    private String _home = null;

    private BrowserContentManager _browserField;
    private RenderingApplication _renderingApplication;

    public WebBrowserScreen() {
        super();

        _renderingApplication = new RenderingApplicationImpl(this);

        _browserField = new BrowserContentManager(NO_VERTICAL_SCROLL);
        /*
        RenderingOptions opt = _browserField.getRenderingSession().getRenderingOptions();
        opt.setProperty(RenderingOptions.CORE_OPTIONS_GUID,
            RenderingOptions.DEFAULT_CHARSET_VALUE,
            "utf-8");
        */

        add(_browserField);
    }

    public void show(String url) {
        _home = url;
        show();
    }

    protected void afterShow(boolean firstShow) {
        if (firstShow)
            navigateTo(_home, null, null);
    }

    protected void makeFormMenu(Menu menu) {
        addBackMenuItem(menu, false);
    }

    //

    private void navigateTo(String url, HttpHeaders headers, byte[] postData) {
        if (StringUtils.isNullOrEmpty(url))
            return;
        if (!beforeNavigate(url))
            return;
        new BrowserThread(this, url, headers, postData).start();
    }

    protected boolean beforeNavigate(String url) {
        return true;
    }

    private void onNavigationError(Throwable ex) {
        UiUtils.alert(ex);
        close();
    }

    // BrowserThread

    private static class BrowserThread extends Thread {

        private WebBrowserScreen _owner;
        private String _url;
        private HttpHeaders _headers;
        private byte[] _postData;

        public BrowserThread(WebBrowserScreen owner, String url, HttpHeaders headers, byte[] postData) {
            _owner = owner;
            _url = url;
            _headers = headers;
            _postData = postData;
        }

        public void run() {
            try {
                String requestUrl = _url + NetworkManager.getInstance().getConnectionOptions();
                //System.out.println("YFrog connecting: " + requestUrl);

                HttpConnection conn = (HttpConnection)Connector.open(requestUrl);
                if (_headers != null) {
                    int size = _headers.size();
                    for (int i = 0; i < size; i++) {
                        String key = _headers.getPropertyKey(i);
                        if (StringUtils.isNullOrEmpty(key)) continue;
                        String value = _headers.getPropertyValue(i);
                        if (value == null) continue;
                        conn.setRequestProperty(key, value);
                    }
                }
                if ((_postData != null) && (_postData.length > 0)) {
                    conn.setRequestMethod(HttpProtocolConstants.HTTP_METHOD_POST);
                    conn.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_LENGTH,
                        Integer.toString(_postData.length));
                    OutputStream os = conn.openOutputStream();
                    try {
                        os.write(_postData);
                        os.flush();
                    }
                    finally {
                        try { os.close(); } catch (Exception ignored) {}
                    }
                }

                _owner._browserField.setContent(
                    conn,
                    _owner._renderingApplication,
                    RenderingConstants.EMBEDDED_CONTENT
                );
            }
            catch (Exception ex) {
                _owner.onNavigationError(ex);
            }
        }
    }

    // RenderingApplicationImpl

    private static class RenderingApplicationImpl implements RenderingApplication {

        private WebBrowserScreen _owner;

        public RenderingApplicationImpl(WebBrowserScreen owner) {
            _owner = owner;
        }

        public Object eventOccurred(Event event) {
            int uid = event.getUID();
            switch (uid) {
                case Event.EVENT_URL_REQUESTED: {
                    UrlRequestedEvent urlEvent = (UrlRequestedEvent)event;
                    String url = urlEvent.getURL();
                    HttpHeaders headers = urlEvent.getHeaders();
                    byte[] postData = urlEvent.getPostData();
                    _owner.navigateTo(url, headers, postData);
                    break;
                }
                case Event.EVENT_REDIRECT: {
                    RedirectEvent redirectEvent = (RedirectEvent)event;
                    String url = redirectEvent.getLocation();
                    _owner.navigateTo(url, null, null);
                    break;
                }
            }

            return null;
        }

        public HttpConnection getResource(RequestedResource requestedresource, BrowserContent browsercontent) {
            try {
                String url = requestedresource.getUrl();
                String requestUrl = url + NetworkManager.getInstance().getConnectionOptions();
                //System.out.println("YFrog connecting: " + requestUrl);
                return (HttpConnection)Connector.open(requestUrl);
            }
            catch (Exception ex) {
                //System.err.println(StringUtils.getExceptionMessage(ex));
                return null;
            }
        }

        public String getHTTPCookie(String s) { return ""; }
        public int getAvailableHeight(BrowserContent browsercontent) { return -1; }
        public int getAvailableWidth(BrowserContent browsercontent) { return -1; }
        public int getHistoryPosition(BrowserContent browsercontent) { return 0; }
        public void invokeRunnable(Runnable runnable) { new Thread(runnable).start(); }
    }
}

