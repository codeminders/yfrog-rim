package codeminders.yfrog.lib.network;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import net.rim.device.api.io.http.*;
import net.rim.device.api.system.*;
import net.rim.device.api.util.*;
import net.rim.device.api.servicebook.*;

import codeminders.yfrog.lib.utils.*;

public class NetworkManager {

    private static final int MAX_THREADS_COUNT = 2;
    private static final int MAX_REDIRECTS = 10;

    private static NetworkManager _instance = null;

    public static synchronized NetworkManager getInstance() {
        if (_instance == null)
            _instance = new NetworkManager();
        return _instance;
    }

    private Object _syncObj = new Object();
    private Vector _listeners = new Vector();

    private long _nextRequestID = 1;
    private SimpleSortingVector _activeRequests = new SimpleSortingVector();

    private NetworkManager() {
        _activeRequests.setSortComparator(new NetworkRequestsComparator());
        _activeRequests.setSort(true);
    }

    public long genID() {
        return _nextRequestID++;
    }

    // Listeners

    public void addListener(NetworkListener listener) {
        synchronized (_listeners) {
            _listeners.removeElement(listener);
            _listeners.addElement(listener);
        }
    }

    public void removeListener(NetworkListener listener) {
        synchronized (_listeners) {
            _listeners.removeElement(listener);
        }
    }

    private void notifyStarted(long id) {
        Vector v;
        synchronized (_listeners) { v = CloneableVector.clone(_listeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).started(id); }
            catch (Exception ignored) {}
    }

    private void notifyComplete(long id, Object result) {
        Vector v;
        synchronized (_listeners) { v = CloneableVector.clone(_listeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).complete(id, result); }
            catch (Exception ignored) {}
    }

    private void notifyError(long id, Throwable ex) {
        Vector v;
        synchronized (_listeners) { v = CloneableVector.clone(_listeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).error(id, ex); }
            catch (Exception ignored) {}
    }

    private void notifyCancelled(long id) {
        Vector v;
        synchronized (_listeners) { v = CloneableVector.clone(_listeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).cancelled(id); }
            catch (Exception ignored) {}
    }

    // Threading

    private static class NetworkRequestsComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            NetworkRequest r1 = (NetworkRequest)o1;
            NetworkRequest r2 = (NetworkRequest)o2;
            long res = (long)r2.getPriority() - (long)r1.getPriority();
            if (res == 0)
                res = r1.getID() - r2.getID();
            return (res < 0) ? -1 : ((res > 0) ? 1 : 0);
        }
    }

    private static class NetworkRequestRedirect implements NetworkRequest {

        private NetworkRequest _owner;
        private String _location;

        public NetworkRequestRedirect(NetworkRequest owner, String location) {
            _owner = owner;
            _location = location;
        }

        public long getID() { return _owner.getID(); }
        public int getPriority() { return _owner.getPriority(); }
        public int getHttpMethod() { return HttpMethod.GET; }
        public String getUrl() { return _location; }
        public ArgumentsList getArgs() { return null; }
        public ContentProvider getPostContent() { return null; }
        public HttpConnectionPreprocessor getPreprocessor() { return null; }
        public ContentProcessor getContentProcessor() { return _owner.getContentProcessor(); }

        public void startNext(
            int httpMethod, String url, ArgumentsList args, ContentProvider postContent,
            HttpConnectionPreprocessor preprocessor, ContentProcessor contentProcessor
        ) {
            _owner.startNext(httpMethod, url, args, postContent, preprocessor, contentProcessor);
        }
    }

    private static class NetworkRequestImpl implements Runnable, NetworkRequest {

        private NetworkManager _owner;
        private long _id;
        private int _priority;
        private int _httpMethod;
        private String _url;
        private ArgumentsList _args;
        private ContentProvider _postContent;
        private HttpConnectionPreprocessor _preprocessor;
        private ContentProcessor _contentProcessor;

        private Thread _t = null;
        private boolean _started = false;
        private boolean _cancelled = false;
        private boolean _restarted = false;

        public NetworkRequestImpl(
            NetworkManager owner,
            long id,
            int priority,
            int httpMethod,
            String url,
            ArgumentsList args,
            ContentProvider postContent,
            HttpConnectionPreprocessor preprocessor,
            ContentProcessor contentProcessor
        ) {
            _owner = owner;
            _id = id;
            _priority = priority;
            _httpMethod = httpMethod;
            _url = url;
            _args = args;
            _postContent = postContent;
            _preprocessor = preprocessor;
            _contentProcessor = contentProcessor;
        }

        public long getID() { return _id; }
        public int getPriority() { return _priority; }
        public int getHttpMethod() { return _httpMethod; }
        public String getUrl() { return _url; }
        public ArgumentsList getArgs() { return _args; }
        public ContentProvider getPostContent() { return _postContent; }
        public HttpConnectionPreprocessor getPreprocessor() { return _preprocessor; }
        public ContentProcessor getContentProcessor() { return _contentProcessor; }

        public void startNext(
            int httpMethod, String url, ArgumentsList args, ContentProvider postContent,
            HttpConnectionPreprocessor preprocessor, ContentProcessor contentProcessor
        ) {
            _httpMethod = httpMethod;
            _url = url;
            _args = args;
            _postContent = postContent;
            _preprocessor = preprocessor;
            _contentProcessor = contentProcessor;
            _restarted = true;
        }

        public boolean isStarted() { return _started; }

        public void start() {
            if (_started)
                return;
            _started = true;
            _t = new Thread(this);
            _t.start();
        }

        public void cancel() {
            _started = true;
            _cancelled = true;
            if (_t != null) {
                _t.interrupt();
                _t = null;
            }
        }

        public void run() {
            _owner.notifyStarted(_id);
            try {
                Object res;
                do {
                    _restarted = false;
                    res = _owner.makeRequest(this, 0);
                } while (_restarted);
                if (!_cancelled)
                    _owner.notifyComplete(_id, res);
            }
            catch (Throwable ex) {
                if (!_cancelled)
                    _owner.notifyError(_id, ex);
            }
            if (_cancelled)
                _owner.notifyCancelled(_id);
            _owner.requestComplete(this);
            _t = null;
        }
    }

    private void processRequestsQueueNoSync() {
        int count = 0;
        int activeRequestsSize = _activeRequests.size();
        for (int i = 0; i < activeRequestsSize; i++)
            if (((NetworkRequestImpl)_activeRequests.elementAt(i)).isStarted())
                count++;
        for (int i = 0; (i < activeRequestsSize) && (count < MAX_THREADS_COUNT); i++) {
            NetworkRequestImpl request = (NetworkRequestImpl)_activeRequests.elementAt(i);
            if (request.isStarted())
                continue;
            request.start();
            count++;
        }
    }

    private void requestComplete(NetworkRequestImpl request) {
        synchronized (_syncObj) {
            _activeRequests.removeElement(request);
            processRequestsQueueNoSync();
        }
    }

    public long startRequest(int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent,
        HttpConnectionPreprocessor preprocessor, ContentProcessor contentProcessor
    ) {
        return startRequest(0, httpMethod, url, args, postContent,
            preprocessor, contentProcessor);
    }

    public long startRequest(int priority, int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent,
        HttpConnectionPreprocessor preprocessor, ContentProcessor contentProcessor
    ) {
        long id;
        synchronized (_syncObj) {
            id = genID();
            NetworkRequestImpl request = new NetworkRequestImpl(this, id, priority,
                httpMethod, url, args, postContent, preprocessor, contentProcessor);
            _activeRequests.addElement(request);
            processRequestsQueueNoSync();
        }
        return id;
    }

    public void cancelRequest(long id) {
        synchronized (_syncObj) {
            int activeRequestsSize = _activeRequests.size();
            for (int i = 0; i < activeRequestsSize; i++) {
                NetworkRequestImpl request = (NetworkRequestImpl)_activeRequests.elementAt(i);
                if (request.getID() == id) {
                    request.cancel();
                    break;
                }
            }
        }
    }

    // Network Request

    private boolean isServiceRecordActive(ServiceRecord rec) {
        if (!rec.isValid())
            return false;
        if (!rec.isDisabled())
            return true;
        String uid = rec.getUid().toLowerCase();
        return ServiceRouting.getInstance().isSerialBypassActive(uid);
    }

    private boolean isWiFiAvailable() {
        /* * /
        ServiceRecord[] recs = ServiceBook.getSB().findRecordsByCid("wptcp");
        if (recs == null)
            return false;
        boolean wifiFound = false;
        int recsLength = recs.length;
        for (int i = 0; i < recsLength; i++) {
            ServiceRecord rec = recs[i];
            if (!isServiceRecordActive(rec))
                continue;
            String uid = rec.getUid().toLowerCase();
            if ((uid.indexOf("wifi") >= 0) || (uid.indexOf("wi-fi") >= 0)) {
                wifiFound = true;
                break;
            }
        }
        if (!wifiFound)
            return false;
        /* */
        if ((RadioInfo.getActiveWAFs() & RadioInfo.WAF_WLAN) == 0)
            return false;
        return CoverageInfo.isCoverageSufficient(
            1/*CoverageInfo.COVERAGE_DIRECT*/, RadioInfo.WAF_WLAN, false);
    }

    private String getServiceBookConnectionOptions() throws IOException {
        if (DeviceInfo.isSimulator())
            return ";deviceside=true"; // Direct TCP
        ServiceRecord[] recs = ServiceBook.getSB().getRecords();
        if (recs == null)
            return ";deviceside=true"; // Direct TCP
            //throw new IOException("No network found");
        boolean mdsFound = false;
        boolean bisbFound = false;
        String wap20Uid = null;
        int recsLength = recs.length;
        for (int i = 0; i < recsLength; i++) {
            ServiceRecord rec = recs[i];
            if (!isServiceRecordActive(rec))
                continue;
            String cid = rec.getCid();
            if (StringUtilities.strEqualIgnoreCase(cid, "IPPP")) {
                if (rec.getEncryptionMode() == ServiceRecord.ENCRYPT_RIM)
                    mdsFound = true;
                else
                    bisbFound = true;
            }
            else if (StringUtilities.strEqualIgnoreCase(cid, "WPTCP")) {
                String uid = rec.getUid();
                if ((uid != null) && (uid.length() > 0)) {
                    if (
                        (uid.toLowerCase().indexOf("wifi") < 0)
                        && (uid.toLowerCase().indexOf("mms") < 0)
                    ) {
                        wap20Uid = uid;
                    }
                }
            }
        }
        if (bisbFound)
            return ";deviceside=false;ConnectionType=mds-public"; // BIS-B
        else if (mdsFound)
            return ";deviceside=false"; // MDS
        else if (wap20Uid != null)
            return ";ConnectionUID=" + wap20Uid; // WAP 2.0
        else
            return ";deviceside=true"; // Direct TCP
            //throw new IOException("No network found");
    }

    public String getConnectionOptions() throws IOException {
        if (isWiFiAvailable())
            return ";deviceside=true;interface=wifi";
        if (RadioInfo.getState() == RadioInfo.STATE_ON)
            return getServiceBookConnectionOptions();
        throw new IOException("Network unavailable");
    }

    public static String formatRequestUrl(String url, ArgumentsList args) {
        String requestUrl = url;
        String argsData = (args != null) ? args.toString() : "";
        if (argsData.length() > 0)
            requestUrl += ("?" + argsData);
        return requestUrl;
    }

    private Object makeRequest(NetworkRequest requestData, int redirectsCount) throws Exception {
        String requestUrl = formatRequestUrl(requestData.getUrl(), requestData.getArgs());
        requestUrl += getConnectionOptions();
        //System.out.println("YFrog connecting: " + requestUrl);
        HttpConnection request = null;
        InputStream is = null;
        try {
            request = (HttpConnection)Connector.open(requestUrl);
            if (request == null)
                throw new NetworkException("Cannot open web request");
            // Prepare connection
            request.setRequestMethod(HttpMethod.toString(requestData.getHttpMethod()));
            if (requestData.getPreprocessor() != null)
                requestData.getPreprocessor().applyToRequest(request);
            // Post data
            if (
                (requestData.getHttpMethod() == HttpMethod.POST || requestData.getHttpMethod() == HttpMethod.PUT)
                && (requestData.getPostContent() != null)
            ) {
                request.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_TYPE,
                    requestData.getPostContent().getContentType());
                request.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_LENGTH,
                    Integer.toString(requestData.getPostContent().getContentLength()));
                String range = requestData.getPostContent().getContentRange();
                if ((range != null) && (range.length() > 0))
                    request.setRequestProperty(HttpProtocolConstants.HEADER_CONTENT_RANGE,
                        range);
                OutputStream os = request.openOutputStream();
                try {
                    requestData.getPostContent().writeContent(os);
                    os.flush();
                }
                finally {
                    try { os.close(); } catch (Exception ignored) {}
                }
            }
            // Process response
            int responseCode = request.getResponseCode();
            is = request.openInputStream();
            switch (responseCode) {
                case HttpConnection.HTTP_OK:
                case HttpConnection.HTTP_CREATED:
                case HttpConnection.HTTP_ACCEPTED: {
                    if (requestData.getContentProcessor() != null) {
                        Hashtable responseHeaders = new Hashtable();
                        for (int i = 0; i < 1000; i++) {
                            String key = request.getHeaderFieldKey(i);
                            if (key == null) break;
                            String value = request.getHeaderField(i);
                            if (value == null) break;
                            responseHeaders.put(key, value);
                        }
                        return requestData.getContentProcessor().process(
                            requestData, responseCode, responseHeaders, is);
                    }
                    else
                        return null;
                }
                case HttpConnection.HTTP_MOVED_PERM:
                case HttpConnection.HTTP_MOVED_TEMP:
                case HttpConnection.HTTP_SEE_OTHER:
                case HttpConnection.HTTP_TEMP_REDIRECT: {
                    if (redirectsCount >= MAX_REDIRECTS)
                        throw new NetworkException("Too many redirects", responseCode,
                            request.getResponseMessage());
                    String location = request.getHeaderField(HttpProtocolConstants.HEADER_LOCATION);
                    if ((location != null) && (location.length() > 0))
                        return makeRequest(
                            new NetworkRequestRedirect(requestData, location),
                            redirectsCount + 1);
                    break;
                }
            }
            // Error
            String responseMessage = request.getResponseMessage();
            String message = null;
            try { message = StreamUtils.readStreamContentToString(is, "UTF-8"); }
            catch (Exception ignored) {}
            if ((message == null) || (message.length() == 0))
                message = responseMessage;
            //System.out.println("Network Error: " + responseCode + " " + responseMessage);
            throw new NetworkException(message.trim(), responseCode, responseMessage);
        }
        catch (Exception ex) {
            String msg = ex.getMessage();
            if ((msg == null) || (msg.length() == 0)) {
                msg = ex.getClass().getName();
                int pos = msg.lastIndexOf('.');
                if (pos >= 0)
                    msg = msg.substring(pos + 1);
            }
            SystemLog.getInstance().addMessage("" + msg + ", \n" + requestUrl);
            throw ex;
        }
        finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (request != null) try { request.close(); } catch (Exception ignored) {}
            //System.out.println("YFrog complete: " + requestUrl);
        }
    }

    // Simple data

    private static class DataContentProcessorImpl implements ContentProcessor {
        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            byte[] data = StreamUtils.readStreamContent(responseData);
            return data;
        }
    }

    public long startDataRequest(String url) {
        return startDataRequest(0, HttpMethod.GET, url, null, null, null);
    }

    public long startDataRequest(int priority, String url) {
        return startDataRequest(priority, HttpMethod.GET, url, null, null, null);
    }

    public long startDataRequest(int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor
    ) {
        return startDataRequest(0, httpMethod, url, args, postContent, preprocessor);
    }

    public long startDataRequest(int priority, int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor
    ) {
        return startRequest(priority, httpMethod, url, args, postContent, preprocessor,
            new DataContentProcessorImpl());
    }

    // Images

    private static class ImageContentProcessorImpl implements ContentProcessor {
        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            byte[] data = StreamUtils.readStreamContent(responseData);
            try {
                return EncodedImage.createEncodedImage(data, 0, data.length);
            }
            catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid picture");
            }
        }
    }

    public long startImageRequest(String url) {
        return startImageRequest(0, HttpMethod.GET, url, null, null, null);
    }

    public long startImageRequest(int priority, String url) {
        return startImageRequest(priority, HttpMethod.GET, url, null, null, null);
    }

    public long startImageRequest(int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor
    ) {
        return startImageRequest(0, httpMethod, url, args, postContent, preprocessor);
    }

    public long startImageRequest(int priority, int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor
    ) {
        return startRequest(priority, httpMethod, url, args, postContent, preprocessor,
            new ImageContentProcessorImpl());
    }

    // File

    private static class FileContentProcessorImpl implements ContentProcessor {

        private String _fileUrl;

        public FileContentProcessorImpl(String fileUrl) {
            _fileUrl = fileUrl;
        }

        public Object process(
            NetworkRequest request,
            int responseCode, Hashtable responseHeaders, InputStream responseData
        ) throws Exception {
            FileConnection conn = null;
            OutputStream os = null;
            try {
                conn = (FileConnection)Connector.open(_fileUrl);
                if (conn.exists())
                    throw new Exception("File exists");
                conn.create();
                os = conn.openOutputStream();
                StreamUtils.copyStreamContent(responseData, os);
            }
            finally {
                if (os != null) try { os.close(); } catch (Exception ignored) {}
                if (conn != null) try { conn.close(); } catch (Exception ignored) {}
            }
            return _fileUrl;
        }
    }

    public long startFileRequest(String url, String fileUrl) {
        return startFileRequest(0, HttpMethod.GET, url, null, null, null, fileUrl);
    }

    public long startFileRequest(int priority, String url, String fileUrl) {
        return startFileRequest(priority, HttpMethod.GET, url, null, null, null, fileUrl);
    }

    public long startFileRequest(int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor,
        String fileUrl
    ) {
        return startFileRequest(0, httpMethod, url, args, postContent, preprocessor, fileUrl);
    }

    public long startFileRequest(int priority, int httpMethod, String url, ArgumentsList args,
        ContentProvider postContent, HttpConnectionPreprocessor preprocessor,
        String fileUrl
    ) {
        return startRequest(priority, httpMethod, url, args, postContent, preprocessor,
            new FileContentProcessorImpl(fileUrl));
    }
}

