package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class StatusItemBase extends ItemBase implements Persistable {

    public long CreatedAt = 0;
    public long ID = 0;
    public String Text = null;
    public String Source = null;
    public boolean Truncated = false;
    public long InReplyToStatusID = 0;
    public long InReplyToUserID = 0;
    public boolean Favorited = false;
    public String InReplyToScreenName = null;

    public GeoItem Geo = null;

    public StatusItemBase() { }

    public void setField(String name, String value) {
        if ("created_at".equals(name)) this.CreatedAt = parseDate(value);
        else if ("id".equals(name)) this.ID = Long.parseLong(value);
        else if ("text".equals(name)) this.Text = parseText(value);
        else if ("source".equals(name)) this.Source = value;
        else if ("truncated".equals(name)) this.Truncated = "true".equals(value);
        else if ("in_reply_to_status_id".equals(name)) this.InReplyToStatusID = Long.parseLong(value);
        else if ("in_reply_to_user_id".equals(name)) this.InReplyToUserID = Long.parseLong(value);
        else if ("favorited".equals(name)) this.Favorited = "true".equals(value);
        else if ("in_reply_to_screen_name".equals(name)) this.InReplyToScreenName = value;
        else super.setField(name, value);
    }

    public ItemBase newChildren(String name) {
        if ("geo".equals(name)) {
            this.Geo = new GeoItem();
            return this.Geo;
        }
        return super.newChildren(name);
    }

    public String toString() { return Text; }
}

