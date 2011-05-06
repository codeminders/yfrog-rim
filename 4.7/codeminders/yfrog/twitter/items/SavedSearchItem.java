package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class SavedSearchItem extends ItemBase implements Persistable {

    public long CreatedAt = 0;
    public long ID = 0;
    public String Name = null;
    public String Query = null;
    public String Position = null;

    public SavedSearchItem() { }

    public void setField(String name, String value) {
        if ("created_at".equals(name)) this.CreatedAt = parseDate(value);
        else if ("id".equals(name)) this.ID = Long.parseLong(value);
        else if ("name".equals(name)) this.Name = parseText(value);
        else if ("query".equals(name)) this.Query = parseText(value);
        else if ("position".equals(name)) this.Position = value;
        else super.setField(name, value);
    }

    public String toString() { return Name; }
}

