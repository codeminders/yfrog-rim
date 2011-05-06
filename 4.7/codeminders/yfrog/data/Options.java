package codeminders.yfrog.data;

import java.util.*;
import net.rim.device.api.system.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.forms.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.lib.network.*;

public class Options {

    public interface MapsApplication {
        public static final int INTERNAL = 0;
        public static final int GOOGLE_MAPS_WEB = 1;
    }

    public interface ImagesLocation {
        public static final int MEMORY = 0;
        public static final int SDCARD = 1;
    }

    private static Options _instance = null;

    public static synchronized Options getInstance() {
        if (_instance == null)
            _instance = new Options();
        return _instance;
    }

    public static void initialize() {
        getInstance();
    }

    // codeminders.yfrog.data.Options.data
    private static final long OPTIONS_DATA_GUID = 0xc90e0eebdd348466L;

    private Object _syncObj = new Object();
    private Hashtable _data = new Hashtable();

    private long _changesCount = 0;
    private int _changesLockCount = 0;

    private Options() {
        load();
    }

    // Load/Save

    private void load() {
        synchronized (_syncObj) {
            // load readonly data
            Hashtable readonlyData = null;
            PersistentObject persist = PersistentStore.getPersistentObject(OPTIONS_DATA_GUID);
            synchronized (persist) {
                try { readonlyData = (Hashtable)persist.getContents(); } catch (Exception ignored) { }
            }
            if (readonlyData == null)
                readonlyData = new Hashtable();
            // copy data
            _data = new Hashtable();
            for (Enumeration e = readonlyData.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                if ((key.length() == 0) || (key.charAt(0) == '$'))
                    continue;
                String value = (String)readonlyData.get(key);
                _data.put(key, value);
            }
            // reset changes
            _changesCount = 0;
        }
        applyValues();
    }

    private void save() {
        synchronized (_syncObj) {
            if (_changesLockCount > 0)
                return;
            if (_changesCount == 0)
                return;
            // create copy without temporary values
            Hashtable newData = new Hashtable();
            for (Enumeration e = _data.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                if ((key.length() == 0) || (key.charAt(0) == '$'))
                    continue;
                String value = (String)_data.get(key);
                newData.put(key, value);
            }
            // custom logic
            if (!getRememberAuth()) {
                newData.remove("Username");
                newData.remove("Password");
                newData.remove("OAuthToken");
                newData.remove("OAuthTokenSecret");
            }
            // save
            try { ObjectGroup.createGroup(newData); } catch (Exception ignored) {}
            PersistentObject persist = PersistentStore.getPersistentObject(OPTIONS_DATA_GUID);
            synchronized (persist) {
                persist.setContents(newData);
                persist.commit();
                //System.out.println("YFrog options: saved");
            }
            // reset changes
            _changesCount = 0;
        }
        applyValues();
    }

    private void applyValues() {
        Authorization auth = null;
        if (isOAuthAuthenticated())
            auth = new OAuthAuthorization(
                getTwitterAppConsumerKey(), getTwitterAppConsumerSecret(),
                getOAuthToken(), getOAuthTokenSecret()
            );
        else if (isBasicAuthenticated())
            auth = new BasicAuthorization(getUsername(), getPassword());
        TwitterManager.getInstance().setAuthorization(auth);
    }

    // Set

    private void set(String key, int[] value) {
        if (value == null) {
            set(key, (String)null);
            return;
        }
        StringBuffer s = new StringBuffer();
        for (int i = 0; i < value.length; i++) {
            if (s.length() > 0) s.append(',');
            s.append(value[i]);
        }
        set(key, s.toString());
    }

    private void set(String key, int value) {
        set(key, Integer.toString(value));
    }

    private void set(String key, boolean value) {
        set(key, value ? "true" : "false");
    }

    private void set(String key, String value) {
        synchronized (_syncObj) {
            String oldValue = (String)_data.get(key);
            if (((oldValue == null) && (value == null)) || StringUtilities.strEqual(oldValue, value))
                return;
            if (value != null)
                _data.put(new String(key), new String(value));
            else
                _data.remove(key);
            _changesCount++;
        }
        save();
    }

    // Get

    private int[] get(String key, int[] def) {
        String value = get(key, (String)null);
        if (value == null)
            return def;
        String[] words = StringUtils.split(value, ',');
        int[] res = new int[words.length];
        for (int i = words.length - 1; i >= 0; i--)
            res[i] = Integer.parseInt(words[i]);
        return res;
    }

    private int get(String key, int def) {
        String value = get(key, (String)null);
        if (value == null)
            return def;
        try { return Integer.parseInt(value); }
        catch (Exception ex) { return def; }
    }

    private boolean get(String key, boolean def) {
        String value = get(key, (String)null);
        if (value == null)
            return def;
        return "true".equals(value);
    }

    private String get(String key, String def) {
        synchronized (_syncObj) {
            String res = (String)_data.get(key);
            if (res == null)
                return def;
            return res;
        }
    }

    // Changes lock

    public void startChanges() {
        synchronized (_syncObj) {
            _changesLockCount++;
        }
    }

    public void endChanges() {
        synchronized (_syncObj) {
            if (_changesLockCount > 0)
                _changesLockCount--;
        }
        save();
    }

    // Options

    public String getYFrogAppKey() { return ResManager.getString(YFrogResource.YFROG_API_KEY); }
    public String getTwitterAppConsumerKey() { return ResManager.getString(YFrogResource.TWITTER_APP_CONSUMER_KEY); }
    public String getTwitterAppConsumerSecret() { return ResManager.getString(YFrogResource.TWITTER_APP_CONSUMER_SECRET); }

    public int getMapsApplication() { return get("MapsApplication", MapsApplication.INTERNAL); }
    public void setMapsApplication(int value) { set("MapsApplication", value); }

    public int getImagesLocation() { return get("ImagesLocation", ImagesLocation.SDCARD); }
    public void setImagesLocation(int value) { set("ImagesLocation", value); }

    // Auth

    public String getUsername() { return get("Username", (String)null); }
    public void setUsername(String value) { set("Username", value); }

    public String getPassword() { return get("Password", (String)null); }
    public String getOAuthToken() { return get("OAuthToken", (String)null); }
    public String getOAuthTokenSecret() { return get("OAuthTokenSecret", (String)null); }

    public boolean getRememberAuth() { return get("RememberAuth", false); }
    public void setRememberAuth(boolean value) { set("RememberAuth", value); }

    public void resetAuth() {
        startChanges();
        try {
            set("Username", (String)null);
            set("Password", (String)null);
            set("OAuthToken", (String)null);
            set("OAuthTokenSecret", (String)null);
        }
        finally {
            endChanges();
        }
    }

    public void setAuthBasic(String username, String password, boolean remember) {
        startChanges();
        try {
            set("Username", username);
            set("Password", password);
            set("OAuthToken", (String)null);
            set("OAuthTokenSecret", (String)null);
            set("RememberAuth", remember);
        }
        finally {
            endChanges();
        }
    }

    public void setAuthOAuth(String username, String token, String tokenSecret, boolean remember) {
        startChanges();
        try {
            set("Username", username);
            set("Password", (String)null);
            set("OAuthToken", token);
            set("OAuthTokenSecret", tokenSecret);
            set("RememberAuth", remember);
        }
        finally {
            endChanges();
        }
    }

    public boolean isAuthenticated() {
        return isBasicAuthenticated() || isOAuthAuthenticated();
    }

    public boolean isBasicAuthenticated() {
        return !(
            StringUtils.isNullOrEmpty(getUsername())
            || StringUtils.isNullOrEmpty(getPassword())
        );
    }

    public boolean isOAuthAuthenticated() {
        return !(
            StringUtils.isNullOrEmpty(getUsername())
            || StringUtils.isNullOrEmpty(getOAuthToken())
            || StringUtils.isNullOrEmpty(getOAuthTokenSecret())
        );
    }

    public boolean isMyself(String value) {
        return StringUtilities.strEqual(value, getUsername());
    }
}

