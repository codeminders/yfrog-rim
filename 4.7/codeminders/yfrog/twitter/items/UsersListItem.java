package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class UsersListItem extends ItemBase implements Persistable {

    public long NextCursor = 0;
    public long PreviousCursor = 0;

    public UsersItem Users = null;

    public UsersListItem() { }

    public void setField(String name, String value) {
        if ("next_cursor".equals(name)) this.NextCursor = Long.parseLong(value);
        else if ("previous_cursor".equals(name)) this.PreviousCursor = Long.parseLong(value);
        else super.setField(name, value);
    }

    public ItemBase newChildren(String name) {
        if ("users".equals(name)) {
            if (this.Users == null)
                this.Users = new UsersItem();
            return this.Users;
        }
        return super.newChildren(name);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Users.Items); }
}

