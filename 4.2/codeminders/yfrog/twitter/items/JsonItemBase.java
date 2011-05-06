package codeminders.yfrog.twitter.items;

import java.util.Enumeration;
import java.util.Hashtable;
import net.rim.device.api.util.Persistable;

public class JsonItemBase implements Persistable {

    protected JsonItemBase() { }

    public final void setFields(Hashtable data) {
        for (Enumeration e = data.keys(); e.hasMoreElements();) {
            Object name = e.nextElement();
            Object value = data.get(name);
            setField(name.toString(), value);
        }
    }

    protected void setField(String name, Object value) { }
}

