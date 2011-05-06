package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class UsersItem extends ItemBase implements Persistable {

    public UserItem[] Items = null;

    public UsersItem() { }

    public ItemBase newChildren(String name) {
        if ("user".equals(name)) {
            if (this.Items == null)
                this.Items = new UserItem[0];
            UserItem user = new UserItem();
            Arrays.add(Items, user);
            return user;
        }
        return super.newChildren(name);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Items); }
}

