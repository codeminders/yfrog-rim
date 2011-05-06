package codeminders.yfrog.lib.utils;

import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.EventLogger;

public class SystemLog {

    // codeminders.yfrog.lib.utils.SystemLog
    private static final long GUID = 0xf8d1356097c4c497L;

    private static SystemLog _instance = null;

    public static synchronized SystemLog getInstance() {
        if (_instance == null)
            _instance = new SystemLog();
        return _instance;
    }

    private long _guid;
    private String _name;

    private SystemLog() {
        _guid = GUID;
        _name = ApplicationDescriptor.currentApplicationDescriptor().getModuleName();
        try { EventLogger.register(_guid, _name, EventLogger.VIEWER_STRING); }
        catch (Exception ignored) {}
    }

    public void addMessage(String msg) {
        try {
            //System.out.println(_name + ": " + msg);
            byte[] data = (msg.length() <= 1024) ? msg.getBytes() : msg.substring(0, 1024).getBytes();
            EventLogger.logEvent(_guid, data);
        }
        catch (Exception ignored) {}
    }
}

