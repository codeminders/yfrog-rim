package codeminders.yfrog.data;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.location.*;
import net.rim.device.api.i18n.*;
import net.rim.device.api.io.*;
import net.rim.device.api.lowmemory.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.data.items.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.lib.utils.*;

public class DataManager implements YFrogResource {

    private static final int MAX_ICONS_COUNT = 300;

    private static DataManager _instance = null;

    public static synchronized DataManager getInstance() {
        if (_instance == null)
            _instance = new DataManager();
        return _instance;
    }

    public static void initialize() {
        getInstance();
    }

    public static void deinitialize() {
        if (_instance == null)
            return;
        _instance.deinitializeInt();
        _instance = null;
    }

    private DataManager() {
        _lowMemoryListener = new LowMemoryListenerImpl(this);

        NetworkManager.getInstance().addListener(new NetworkListenerImpl(this));
        LowMemoryManager.addLowMemoryListener(_lowMemoryListener);
        loadUnsentQueue();
    }

    private void deinitializeInt() {
        LowMemoryManager.removeLowMemoryListener(_lowMemoryListener);
    }

    // Date format

    private DateFormat _timeFormat = null;
    private DateFormat _dateFormatNoYear = null;
    private DateFormat _dateFormatFull = null;

    private DateFormat getTimeFormat() {
        if (_timeFormat == null)
            _timeFormat = DateFormat.getInstance(DateFormat.TIME_DEFAULT);
        return _timeFormat;
    }

    private DateFormat getDateFormatNoYear() {
        if (_dateFormatNoYear == null)
            _dateFormatNoYear = new SimpleDateFormat("d MMM");
        return _dateFormatNoYear;
    }

    private DateFormat getDateFormatFull() {
        if (_dateFormatFull == null)
            _dateFormatFull = new SimpleDateFormat("d MMM yyyy");
        return _dateFormatFull;
    }

    public String formatDate(long date) {
        int days = DateUtils.getDaysToToday(date);
        StringBuffer res = new StringBuffer();
        switch (days) {
            case 0:
                break;
            case 1:
                res.append(ResManager.getString(LABEL_YESTERDAY));
                res.append(' ');
                break;
            default:
                if (DateUtils.isTodayYear(date))
                    res.append(getDateFormatNoYear().formatLocal(date));
                else
                    res.append(getDateFormatFull().formatLocal(date));
                res.append(' ');
        }
        res.append(getTimeFormat().formatLocal(date));
        return res.toString();
    }

    // Unsent queue

    // codeminders.yfrog.data.Data.unsentQueue
    private static final long UNSENT_QUEUE_GUID = 0x5011451eec375c71L;

    private Object _unsentQueueSyncObj = new Object();
    private Vector _unsentQueue = null;
    private Vector _unsentQueueListeners = new Vector();

    public void addUnsentListener(DataListener listener) {
        synchronized (_unsentQueueListeners) {
            _unsentQueueListeners.removeElement(listener);
            _unsentQueueListeners.addElement(listener);
        }
    }

    public void removeUnsentListener(DataListener listener) {
        synchronized (_unsentQueueListeners) {
            _unsentQueueListeners.removeElement(listener);
        }
    }

    private void notifyUnsentItemAdded(UnsentItemBase item) {
        Vector v;
        synchronized (_unsentQueueListeners) { v = CloneableVector.clone(_unsentQueueListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((DataListener)e.nextElement()).itemAdded(item); }
            catch (Exception ignored) {}
    }

    private void notifyUnsentItemRemoved(UnsentItemBase item) {
        Vector v;
        synchronized (_unsentQueueListeners) { v = CloneableVector.clone(_unsentQueueListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((DataListener)e.nextElement()).itemRemoved(item); }
            catch (Exception ignored) {}
    }

    private void loadUnsentQueue() {
        PersistentObject persist = PersistentStore.getPersistentObject(UNSENT_QUEUE_GUID);
        synchronized (persist) {
            try { _unsentQueue = (Vector)persist.getContents(); } catch (Exception ignored) { }
        }
        if (_unsentQueue == null)
            _unsentQueue = new Vector();
    }

    private void saveUnsentQueue() {
        PersistentObject persist = PersistentStore.getPersistentObject(UNSENT_QUEUE_GUID);
        synchronized (persist) {
            persist.setContents(_unsentQueue);
            persist.commit();
            //System.out.println("YFrog unsent queue: saved");
        }
    }

    public void addUnsentItem(UnsentItemBase item) {
        synchronized (_unsentQueueSyncObj) {
            try { ObjectGroup.createGroup(item); } catch (Exception ignored) {}
            _unsentQueue.removeElement(item);
            _unsentQueue.addElement(item);
            saveUnsentQueue();
        }
        notifyUnsentItemAdded(item);
    }

    public void removeUnsentItem(UnsentItemBase item) {
        synchronized (_unsentQueueSyncObj) {
            _unsentQueue.removeElement(item);
            saveUnsentQueue();
        }
        notifyUnsentItemRemoved(item);
    }

    public UnsentItemBase[] getUnsentItems() {
        String senderName = Options.getInstance().getUsername();
        synchronized (_unsentQueueSyncObj) {
            Vector resList = new Vector();
            int size = _unsentQueue.size();
            for (int i = 0; i < size; i++) {
                UnsentItemBase item = (UnsentItemBase)_unsentQueue.elementAt(i);
                if (StringUtilities.strEqual(senderName, item.SenderScreenName))
                    resList.addElement(item);
            }
            UnsentItemBase[] res = new UnsentItemBase[resList.size()];
            resList.copyInto(res);
            return res;
        }
    }

    // NetworkListenerImpl

    private static class NetworkListenerImpl implements NetworkListener {

        private DataManager _owner;

        public NetworkListenerImpl(DataManager owner) {
            _owner = owner;
        }

        public void started(long id) {
            if (_owner.isSendRequest(id))
                _owner.startSendRequest(id);
        }

        public void complete(long id, Object result) {
            if (_owner.isIconRequest(id))
                _owner.completeIconRequest(id, result);
            else if (_owner.isSendRequest(id))
                _owner.completeSendRequest(id, result);
        }

        public void error(long id, Throwable ex) {
            if (_owner.isIconRequest(id))
                _owner.errorIconRequest(id, ex);
            else if (_owner.isSendRequest(id))
                _owner.errorSendRequest(id, ex);
        }

        public void cancelled(long id) {
            if (_owner.isIconRequest(id))
                _owner.removeIconRequest(id);
            else if (_owner.isSendRequest(id))
                _owner.cancelSendRequest(id);
        }
    }

    // LowMemoryListenerImpl

    private LowMemoryListener _lowMemoryListener;

    private static class LowMemoryListenerImpl implements LowMemoryListener {

        private DataManager _owner;

        public LowMemoryListenerImpl(DataManager owner) {
            _owner = owner;
        }

        public boolean freeStaleObject(int priority) {
            _owner.cleanupImagesCache();
            return false;
        }
    }

    // Images

    private static class ImageRequest {
        public long ID;
        public String URL;
        public ImageRequest(long id, String url) {
            this.ID = id;
            this.URL = url;
        }
    }

    private void cleanupImagesCache() {
        synchronized (_icons) {
            _icons.clear();
            _iconsHistory.removeAllElements();
        }
    }

    // Icons

    private Vector _iconListeners = new Vector();
    private Hashtable _icons = new Hashtable();
    private Vector _iconsHistory = new Vector();
    private LongHashtable _iconRequests = new LongHashtable();

    public void addIconListener(ImageDataListener listener) {
        synchronized (_iconListeners) {
            _iconListeners.removeElement(listener);
            _iconListeners.addElement(listener);
        }
    }

    public void removeIconListener(ImageDataListener listener) {
        synchronized (_iconListeners) {
            _iconListeners.removeElement(listener);
        }
    }

    private void notifyIconLoaded(long requestID, String url, EncodedImage image) {
        Vector v;
        synchronized (_iconListeners) { v = CloneableVector.clone(_iconListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((ImageDataListener)e.nextElement()).imageLoaded(requestID, url, image); }
            catch (Exception ignored) {}
    }

    private boolean isIconRequest(long id) {
        synchronized (_icons) { return _iconRequests.containsKey(id); }
    }

    private void removeIconRequest(long id) {
        synchronized (_icons) { _iconRequests.remove(id); }
    }

    private void completeIconRequest(long id, Object result) {
        synchronized (_icons) {
            ImageRequest ir = (ImageRequest)_iconRequests.get(id);
            if (ir != null) {
                _iconRequests.remove(id);
                while (_iconsHistory.size() >= MAX_ICONS_COUNT) {
                    String url = (String)_iconsHistory.elementAt(0);
                    _iconsHistory.removeElementAt(0);
                    _icons.remove(url);
                }
                _icons.put(ir.URL, result);
                _iconsHistory.removeElement(ir.URL);
                _iconsHistory.addElement(ir.URL);
                notifyIconLoaded(id, ir.URL, (EncodedImage)result);
                return;
            }
        }
    }

    private void errorIconRequest(long id, Throwable ex) {
        if (
            (ex instanceof IllegalArgumentException)
            || (
                (ex instanceof NetworkException)
                && (((NetworkException)ex).getResponseCode() == HttpConnection.HTTP_FORBIDDEN)
            )
        )
            completeIconRequest(id, EncodedImage.getEncodedImageResource("invalid.png"));
        else
            removeIconRequest(id);
    }

    public EncodedImage loadIcon(String url) {
        synchronized (_icons) {
            EncodedImage img = (EncodedImage)_icons.get(url);
            if (img == null) {
                synchronized (_icons) {
                    for (Enumeration e = _iconRequests.elements(); e.hasMoreElements();)
                        if (StringUtilities.strEqual(((ImageRequest)e.nextElement()).URL, url))
                            return null;
                    long id = NetworkManager.getInstance().startImageRequest(-1, url);
                    ImageRequest ir = new ImageRequest(id, url);
                    _iconRequests.put(id, ir);
                }
                return null;
            }
            _iconsHistory.removeElement(url);
            _iconsHistory.addElement(url);
            return img;
        }
    }

    // Files

    private Vector _imageFileListeners = new Vector();
    private Vector _imageFileQueue = new Vector();
    private Thread _imageFileThread = null;

    public void addImageFileListener(ImageDataListener listener) {
        synchronized (_imageFileListeners) {
            _imageFileListeners.removeElement(listener);
            _imageFileListeners.addElement(listener);
        }
    }

    public void removeImageFileListener(ImageDataListener listener) {
        synchronized (_imageFileListeners) {
            _imageFileListeners.removeElement(listener);
        }
    }

    private void notifyImageFileLoaded(String url, EncodedImage image) {
        Vector v;
        synchronized (_imageFileListeners) { v = CloneableVector.clone(_imageFileListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((ImageDataListener)e.nextElement()).imageLoaded(-1, url, image); }
            catch (Exception ignored) {}
    }

    private void imageFileThreadProc() {
        while (true) {
            String url;
            synchronized (_imageFileQueue) {
                if (_imageFileQueue.size() == 0) {
                    _imageFileThread = null;
                    return;
                }
                url = (String)_imageFileQueue.elementAt(0);
            }
            try {
                FileConnection conn = (FileConnection)Connector.open(url);
                InputStream in = null;
                try {
                    if (!conn.exists() || conn.isDirectory())
                        continue;
                    in = conn.openInputStream();
                    byte[] data = StreamUtils.readStreamContent(in);
                    EncodedImage img = EncodedImage.createEncodedImage(data, 0, data.length);
                    notifyImageFileLoaded(url, img);
                }
                finally {
                    if (in != null) try { in.close(); } catch (Exception ignored) {}
                    try { conn.close(); } catch (Exception ignored) {}
                }
            }
            catch (Exception ex) {
            }
            finally {
                synchronized (_imageFileQueue) {
                    _imageFileQueue.removeElement(url);
                    if (_imageFileQueue.size() == 0) {
                        _imageFileThread = null;
                        return;
                    }
                }
            }
        }
    }

    public void loadImageFile(String url) {
        synchronized (_imageFileQueue) {
            if (_imageFileQueue.indexOf(url) >= 0)
                return;
            _imageFileQueue.addElement(url);
            if (_imageFileThread == null) {
                _imageFileThread = new Thread(new Runnable() {
                    public void run() { imageFileThreadProc(); }
                });
                _imageFileThread.setPriority(Thread.MIN_PRIORITY);
                _imageFileThread.start();
            }
        }
    }

    public void cancelLoadImageFile(String url) {
        synchronized (_imageFileQueue) {
            _imageFileQueue.removeElement(url);
        }
    }

    // Location

    private static final long LOCATION_VALID_TIME = 60L * 1000L; // 1 min

    private Object _locationSyncObj = new Object();
    private Thread _locationThread = null;
    private LocationProvider _locationProvider = null;
    private Location _lastLocation = null;

    private Vector _locationListeners = new Vector();

    public void addLocationListener(LocationDataListener listener) {
        synchronized (_locationListeners) {
            _locationListeners.removeElement(listener);
            _locationListeners.addElement(listener);
        }
    }

    public void removeLocationListener(LocationDataListener listener) {
        synchronized (_locationListeners) {
            _locationListeners.removeElement(listener);
        }
    }

    private void notifyLocationReceived(Location location) {
        Vector v;
        synchronized (_locationListeners) { v = CloneableVector.clone(_locationListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((LocationDataListener)e.nextElement()).locationReceived(location); }
            catch (Exception ignored) {}
    }

    private void notifyLocationError(Throwable ex) {
        Vector v;
        synchronized (_locationListeners) { v = CloneableVector.clone(_locationListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((LocationDataListener)e.nextElement()).locationError(ex); }
            catch (Exception ignored) {}
    }

    private void notifyLocationCancelled() {
        Vector v;
        synchronized (_locationListeners) { v = CloneableVector.clone(_locationListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((LocationDataListener)e.nextElement()).locationCancelled(); }
            catch (Exception ignored) {}
    }

    private void locationThreadProc() {
        boolean providerCreated = false;
        Location location = null;
        Exception error = null;
        try {
            _locationProvider = LocationProvider.getInstance(null);
            if (_locationProvider == null)
                throw new Exception("Cannot retrieve location provider");
            providerCreated = true;
            location = _locationProvider.getLocation(-1);
            synchronized (_locationSyncObj) { if (_locationProvider == null) return; }
            if (
                (location == null)
                || (!location.isValid())
                || (location.getTimestamp() <= 0)
                || (location.getQualifiedCoordinates() == null)
                || (
                    (location.getQualifiedCoordinates().getLatitude() == 0.0)
                    && (location.getQualifiedCoordinates().getLongitude() == 0.0)
                )
            )
                throw new LocationException("Invalid location");
        }
        catch (Exception ex) {
            error = ex;
        }
        finally {
            boolean cancelled;
            synchronized (_locationSyncObj) {
                _locationThread = null;
                cancelled = providerCreated && (_locationProvider == null);
            }
            if (cancelled)
                notifyLocationCancelled();
            else if ((location == null) || (error != null))
                notifyLocationError(error);
            else {
                synchronized (_locationSyncObj) { _lastLocation = location; }
                notifyLocationReceived(location);
            }
        }
    }

    public boolean isLocationEnabled() {
        try {
            LocationProvider locationProvider = LocationProvider.getInstance(null);
            if (locationProvider == null)
                return false;
            int state = locationProvider.getState();
            if (state == LocationProvider.AVAILABLE)
                return true;
        }
        catch (Exception ex) {
        }
        return false;
    }

    public void startReceiveLocation() {
        Location location = null;
        synchronized (_locationSyncObj) { location = _lastLocation; }
        if (
            (location != null)
            && ((location.getTimestamp() + LOCATION_VALID_TIME) >= System.currentTimeMillis())
        ) {
            UiApplication.getUiApplication().invokeLater(new RunnableImpl(location) {
                public void run() { notifyLocationReceived((Location)data0); }
            });
            return;
        }

        synchronized (_locationSyncObj) {
            if (_locationThread != null)
                return;
            _locationThread = new Thread(new Runnable() {
                public void run() { locationThreadProc(); }
            });
            _locationThread.start();
        }
    }

    public void cancelReceiveLocation() {
        synchronized (_locationSyncObj) {
            if (_locationProvider != null) {
                LocationProvider prov = _locationProvider;
                _locationProvider = null;
                prov.reset();
            }
        }
    }

    // Send message

    public static final String YFROG_STUB_URL = "http://yfrog.com/image.jpg";

    private static final String TINYURL_ALIAS = "y";
    private static final String TINYURL_URL = "http://tinyurl.com/" + TINYURL_ALIAS;
    private static final String TINYURL_CREATE_URL = "http://tinyurl.com/create.php";

    public static String generateTinyUrl() {
        return TINYURL_URL + Long.toString(System.currentTimeMillis(), 16).toLowerCase();
    }

    public static String generateGoogleMapsUrl(double latitude, double longitude) {
        return "http://maps.google.com/maps?q="
            + StringUtils.formatDouble(latitude, 6, 1) + ","
            + StringUtils.formatDouble(longitude, 6, 1);
    }

    private static long uploadMedia(String fileUrl, Double latitude, Double longitude) throws IOException {
        String[] tags = null;
        if ((latitude != null) && (longitude != null))
            tags = new String[] {
                "geotagged",
                "geo:lat=" + StringUtils.formatDouble(latitude.doubleValue(), 10, 1),
                "geo:lon=" + StringUtils.formatDouble(longitude.doubleValue(), 10, 1)
            };

        byte[] data;
        FileConnection conn = null;
        InputStream is = null;
        try {
            conn = (FileConnection)Connector.open(fileUrl);
            is = conn.openInputStream();
            data = StreamUtils.readStreamContent(is);
        }
        finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
        YFrogConnector yfrog = new YFrogConnector(
            Options.getInstance().getYFrogAppKey());
        if (FileUtils.isVideoFile(fileUrl))
            return yfrog.YFROGuploadByChunks(
                data,
                FileUtils.getFilename(fileUrl),
                null,
                Options.getInstance().getUsername(),
                null,//Options.getInstance().getPassword(),
                tags,
                true
            );
        else {
            boolean oauth = Options.getInstance().isOAuthAuthenticated();
            return yfrog.YFROGupload(
                data,
                //MIMETypeAssociations.getMIMEType(FileUtils.getFilename(fileUrl)),
                null,
                Options.getInstance().getUsername(),
                Options.getInstance().getPassword(),
                oauth,
                oauth ? TwitterManager.getInstance().getUrlAccountVerifyCredentials() : null,
                tags,
                true
            );
        }
    }

    private static long uploadTinyUrl(String url, String alias) {
        ArgumentsList args = new ArgumentsList();
        args.set("alias", alias);
        args.set("url", url);
        return NetworkManager.getInstance().startRequest(
            HttpMethod.POST, TINYURL_CREATE_URL, null,
            new ContentXWwwFormUrlEncoded(args), null, null
        );
    }

    private Vector _sendListeners = new Vector();
    private Vector _sendItems = new Vector();

    public void addSendListener(NetworkListener listener) {
        synchronized (_sendListeners) {
            _sendListeners.removeElement(listener);
            _sendListeners.addElement(listener);
        }
    }

    public void removeSendListener(NetworkListener listener) {
        synchronized (_sendListeners) {
            _sendListeners.removeElement(listener);
        }
    }

    private void notifySendStarted(long id) {
        Vector v;
        synchronized (_sendListeners) { v = CloneableVector.clone(_sendListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).started(id); }
            catch (Exception ignored) {}
    }

    private void notifySendComplete(long id, Object result) {
        Vector v;
        synchronized (_sendListeners) { v = CloneableVector.clone(_sendListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).complete(id, result); }
            catch (Exception ignored) {}
    }

    private void notifySendError(long id, Throwable ex) {
        Vector v;
        synchronized (_sendListeners) { v = CloneableVector.clone(_sendListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).error(id, ex); }
            catch (Exception ignored) {}
    }

    private void notifySendCancelled(long id) {
        Vector v;
        synchronized (_sendListeners) { v = CloneableVector.clone(_sendListeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            try { ((NetworkListener)e.nextElement()).cancelled(id); }
            catch (Exception ignored) {}
    }

    private static class SendItem {

        private static final int STAGE_START = 1;

        private static final int STAGE_TEXT_LOCATION = 1;
        private static final int STAGE_MY_LOCATION   = 2;
        private static final int STAGE_IMAGE         = 3;
        private static final int STAGE_TEXT          = 4;
        private static final int STAGE_COMPLETE      = 5;

        private UnsentItemBase _item;
        private String _text;

        private long _id = -1;
        private long _firstResuestID = -1;
        private long _requestID = -1;
        private int _stage = -1;
        private Object _result = null;

        public SendItem(UnsentItemBase item) {
            _item = item;
            _text = item.Text;
            _id = NetworkManager.getInstance().genID();
        }

        public long getID() { return _id; }
        public long getFirstRequestID() { return _firstResuestID; }
        public long getCurrentRequestID() { return _requestID; }
        public boolean isComplete() { return _stage >= STAGE_COMPLETE; }
        public Object getResult() { return _result; }

        public void start() throws Exception {
            _stage = STAGE_START;
            startStage();
            _firstResuestID = _requestID;
        }

        private void startStage() throws Exception {
            for (; _stage < STAGE_COMPLETE; _stage++)
                switch (_stage) {
                    case STAGE_TEXT_LOCATION: if (startUploadTextLocation()) return; break;
                    case STAGE_MY_LOCATION: if (startUploadMyLocation()) return; break;
                    case STAGE_IMAGE: if (startUploadImage()) return; break;
                    case STAGE_TEXT: startSendText(); return;
                }
        }

        public void completeRequest(Object result) throws Exception {
            _result = result;
            switch (_stage) {
                case STAGE_IMAGE: completeUploadImage(result); break;
            }
            _stage++;
            startStage();
        }

        private boolean startUploadTextLocation() {
            if (!_item.hasTextLocation())
                return false;
            String alias = StringUtils.replaceString(_item.TextLocationUrl, TINYURL_URL, TINYURL_ALIAS);
            String url = generateGoogleMapsUrl(
                _item.getTextLatitude().doubleValue(),
                _item.getTextLongitude().doubleValue()
            );
            _requestID = uploadTinyUrl(url, alias);
            return true;
        }

        private boolean startUploadMyLocation() {
            if (!_item.hasProfileLocation())
                return false;
            String location =
                StringUtils.formatDouble(_item.getProfileLatitude().doubleValue(), 6, 1)
                + ","
                + StringUtils.formatDouble(_item.getProfileLongitude().doubleValue(), 6, 1);
            _requestID = TwitterManager.getInstance().accountUpdateProfile(null, null, location, null);
            return true;
        }

        private boolean startUploadImage() throws Exception {
            if (StringUtils.isNullOrEmpty(_item.MediaFile))
                return false;
            _requestID = uploadMedia(
                _item.MediaFile,
                _item.getMediaLatitude(),
                _item.getMediaLongitude()
            );
            return true;
        }

        private void completeUploadImage(Object result) throws Exception {
            if (result == null)
                return;
            String url = null;
            if (result instanceof ImgInfo) {
                ImgInfo info = (ImgInfo)result;
                if (info.links != null)
                    url = info.links.yfrog_link;
            }
            else if (result instanceof Response) {
                Response info = (Response)result;
                url = info.mediaurl;
            }
            if ((url == null) || (url.length() == 0))
                throw new IllegalArgumentException("Unexpected response");
            _text = StringUtils.replaceString(_text, YFROG_STUB_URL,
                url);
        }

        private void startSendText() throws Exception {
            if (_item instanceof UnsentStatusItem)
                _requestID = TwitterManager.getInstance().statusesUpdate(
                    _text, _item.getTextLatitude(), _item.getTextLongitude(),
                    ((UnsentStatusItem)_item).InReplyToStatusID
                );
            else if (_item instanceof UnsentDirectMessageItem)
                _requestID = TwitterManager.getInstance().directMessagesNew(
                    ((UnsentDirectMessageItem)_item).RecipientScreenName,
                    _text
                );
        }
    }

    private SendItem getSendRequest(long networkRequestID) {
        synchronized (_sendItems) {
            if (_sendItems.size() == 0)
                return null;
            for (Enumeration e = _sendItems.elements(); e.hasMoreElements();) {
                SendItem si = (SendItem)e.nextElement();
                if (networkRequestID == si.getCurrentRequestID())
                    return si;
            }
        }
        return null;
    }

    private boolean isSendRequest(long id) {
        return (getSendRequest(id) != null);
    }

    private void startSendRequest(long id) {
        SendItem si = getSendRequest(id);
        if ((si != null) && (id == si.getFirstRequestID()))
            notifySendStarted(si.getID());
    }

    private void completeSendRequest(long id, Object result) {
        SendItem si = getSendRequest(id);
        if (si != null) {
            try {
                si.completeRequest(result);
            }
            catch (Exception ex) {
                synchronized (_sendItems) { _sendItems.removeElement(si); }
                notifySendError(si.getID(), ex);
                return;
            }
            if (si.isComplete()) {
                synchronized (_sendItems) { _sendItems.removeElement(si); }
                notifySendComplete(si.getID(), si.getResult());
            }
        }
    }

    private void errorSendRequest(long id, Throwable ex) {
        SendItem si = getSendRequest(id);
        if (si != null) {
            synchronized (_sendItems) { _sendItems.removeElement(si); }
            notifySendError(si.getID(), ex);
        }
    }

    private void cancelSendRequest(long id) {
        SendItem si = getSendRequest(id);
        if (si != null) {
            synchronized (_sendItems) { _sendItems.removeElement(si); }
            notifySendCancelled(si.getID());
        }
    }

    public long send(UnsentItemBase item) throws Exception {
        SendItem si = new SendItem(item);
        synchronized (_sendItems) { _sendItems.addElement(si); }
        try {
            si.start();
        }
        catch (Exception ex) {
            synchronized (_sendItems) { _sendItems.removeElement(si); }
            throw ex;
        }
        return si.getID();
    }
}

