package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class DirectMessagesItem extends ItemBase implements Persistable {

    public DirectMessageItem[] Items = null;

    public DirectMessagesItem() { }

    public ItemBase newChildren(String name) {
        if ("direct_message".equals(name)) {
            if (this.Items == null)
                this.Items = new DirectMessageItem[0];
            DirectMessageItem msg = new DirectMessageItem();
            Arrays.add(Items, msg);
            return msg;
        }
        return super.newChildren(name);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Items); }
}

