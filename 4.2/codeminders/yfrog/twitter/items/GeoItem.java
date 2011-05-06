package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class GeoItem extends ItemBase implements Persistable {

    public String Point = null;

    public GeoItem() { }

    public void setField(String name, String value) {
        if ("georss:point".equals(name)) this.Point = parseText(value);
        else super.setField(name, value);
    }

    public String toString() { return Point; }
}

