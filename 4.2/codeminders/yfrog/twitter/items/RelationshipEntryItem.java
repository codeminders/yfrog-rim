package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class RelationshipEntryItem extends ItemBase implements Persistable {

    public long ID;
    public String ScreenName;
    public boolean Following;
    public boolean FollowedBy;
    public boolean NotificationsEnabled;

    public RelationshipEntryItem() { }

    public void setField(String name, String value) {
        if ("id".equals(name)) this.ID = Long.parseLong(value);
        else if ("screen_name".equals(name)) this.ScreenName = value;
        else if ("following".equals(name)) this.Following = "true".equals(value);
        else if ("followed_by".equals(name)) this.FollowedBy = "true".equals(value);
        else if ("notifications_enabled".equals(name)) this.NotificationsEnabled = "true".equals(value);
        else super.setField(name, value);
    }

    public String toString() { return ScreenName; }
}

