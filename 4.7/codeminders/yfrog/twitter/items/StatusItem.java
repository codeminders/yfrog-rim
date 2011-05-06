package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class StatusItem extends StatusItemBase implements Persistable {

    public UserItem User = null;

    public StatusItem() { }

    public ItemBase newChildren(String name) {
        if ("user".equals(name)) {
            this.User = new UserItem();
            return this.User;
        }
        return super.newChildren(name);
    }

    public String toString() {
        return Text;
    }
}

