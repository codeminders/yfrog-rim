package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class DirectMessageItem extends ItemBase implements Persistable {

    public long CreatedAt = 0;
    public long ID = 0;
    public String Text = null;
    public long SenderID = 0;
    public long RecipientID = 0;
    public String SenderScreenName = null;
    public String RecipientScreenName = null;

    public UserItemBase Sender = null;
    public UserItemBase Recipient = null;

    public DirectMessageItem() { }

    public void setField(String name, String value) {
        if ("created_at".equals(name)) this.CreatedAt = parseDate(value);
        else if ("id".equals(name)) this.ID = Long.parseLong(value);
        else if ("text".equals(name)) this.Text = parseText(value);
        else if ("sender_id".equals(name)) this.SenderID = Long.parseLong(value);
        else if ("recipient_id".equals(name)) this.RecipientID = Long.parseLong(value);
        else if ("sender_screen_name".equals(name)) this.SenderScreenName = value;
        else if ("recipient_screen_name".equals(name)) this.RecipientScreenName = value;
        else super.setField(name, value);
    }

    public ItemBase newChildren(String name) {
        if ("sender".equals(name)) {
            this.Sender = new UserItemBase();
            return this.Sender;
        }
        if ("recipient".equals(name)) {
            this.Recipient = new UserItemBase();
            return this.Recipient;
        }
        return super.newChildren(name);
    }

    public String toString() { return Text; }
}

