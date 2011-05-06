package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class UserItem extends UserItemBase implements Persistable  {

    public StatusItem Status = null;

    public UserItem() { }

    public ItemBase newChildren(String name) {
        if ("status".equals(name)) {
            this.Status = new StatusItem();
            return this.Status;
        }
        return super.newChildren(name);
    }
}

