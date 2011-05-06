package codeminders.yfrog.utils;

import java.util.*;
import javax.microedition.io.file.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.util.*;

public class SysUtils {

    public static final long OS400 = 0x0004000000000000L;
    public static final long OS402 = 0x0004000000020000L;
    public static final long OS410 = 0x0004000100000000L;
    public static final long OS420 = 0x0004000200000000L;
    public static final long OS421 = 0x0004000200010000L;
    public static final long OS430 = 0x0004000300000000L;
    public static final long OS450 = 0x0004000500000000L;
    public static final long OS460 = 0x0004000600000000L;
    public static final long OS470 = 0x0004000700000000L;
    public static final long OS500 = 0x0005000000000000L;

    private static String osVersion = null;
    private static long osVersionNum = 0;

    private static String devicePIN =
        Integer.toHexString(DeviceInfo.getDeviceId()).toUpperCase();

    private static void obtainOSVersion() {
        if (osVersion != null)
            return;

        osVersion = "0.0.0.0";
        int[] modHandles = CodeModuleManager.getModuleHandles();
        for(int i = 0; i < modHandles.length; i++) {
            String modName = CodeModuleManager.getModuleName(modHandles[i]);
            if ("net_rim_os".equals(modName)) {
                osVersion = CodeModuleManager.getModuleVersion(modHandles[i]);
                break;
            }
        }

        String[] values = StringUtils.split(osVersion, new char[] { '.' });
        osVersionNum = 0;
        for (int i = 0; i < 4; i++) {
            long v = 0;
            if ((i < values.length)/* && (i < 3)*/)
                try { v = Long.parseLong(values[i]); } catch (Exception ex) { }
            if (v > 0x0fff)
                v = 0x0fff;
            osVersionNum = osVersionNum << 16;
            osVersionNum |= v;
        }
        if (osVersionNum < 0)
            osVersionNum = Long.MAX_VALUE;
    }

    public static String getOSVersion() {
        obtainOSVersion();
        return osVersion;
    }

    public static String getOSVersion(int digits) {
        StringBuffer res = new StringBuffer();
        String[] values = StringUtils.split(getOSVersion(), new char[] { '.' });
        for (int i = 0; (i < digits) && (i < values.length); i++) {
            if (i > 0)
                res.append('.');
            res.append(values[i]);
        }
        return res.toString();
    }

    public static long getOSVersionNum() {
        obtainOSVersion();
        return osVersionNum;
    }

    public static String getDevicePIN() {
        return devicePIN;
    }

    public static boolean inStartup() {
        return ApplicationManager.getApplicationManager().inStartup();
    }

    public static boolean isTouchScreenSupported() {
        /* VERSION_DEPENDED: 4.7 */
        return Touchscreen.isSupported();
        /* */
        /* VERSION_DEPENDED_NOT: 4.7 * /
        return false;
        /* */
    }

    // Current app

    public static String currentModuleName() {
        return ApplicationDescriptor.currentApplicationDescriptor().getModuleName();
    }

    // SD Card

    public static boolean isSDCardAvaiable() {
        for (Enumeration e = FileSystemRegistry.listRoots(); e.hasMoreElements();) {
            if (StringUtilities.strEqualIgnoreCase("sdcard/", e.nextElement().toString()))
                return true;
        }
        return false;
    }
}

