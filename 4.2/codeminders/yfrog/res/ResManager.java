package codeminders.yfrog.res;

import java.io.InputStream;
import java.io.InputStreamReader;
import net.rim.device.api.i18n.*;

import codeminders.yfrog.lib.utils.StreamUtils;
import codeminders.yfrog.utils.StringUtils;

public class ResManager {

    private ResManager() { }

    private static ResourceBundle _resources = null;

    public static ResourceBundle getResourceBundle() {
        if (_resources == null) {
            ResourceBundleFamily resFamily = ResourceBundle.getBundle(
                YFrogResource.BUNDLE_ID, YFrogResource.BUNDLE_NAME);
            //_resources = resFamily.getBundle(Locale.getDefault());
            //if (_resources == null)
                _resources = resFamily;
        }
        return _resources;
    }

    public static String getString(int key) {
        return getResourceBundle().getString(key);
    }

    public static String getString(int fmtKey, Object[] values) {
        return MessageFormat.format(getResourceBundle().getString(fmtKey), values);
    }

    public static String getString(int fmtKey, Object value0) {
        return getString(fmtKey, new Object[] { value0 });
    }

    public static String getString(int fmtKey, Object value0, Object value1) {
        return getString(fmtKey, new Object[] { value0, value1 });
    }
}

