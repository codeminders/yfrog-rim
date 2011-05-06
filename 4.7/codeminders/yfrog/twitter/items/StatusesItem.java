package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class StatusesItem extends ItemBase implements Persistable {

    public StatusItem[] Items = null;

    public StatusesItem() { }

    public ItemBase newChildren(String name) {
        if ("status".equals(name)) {
            if (this.Items == null)
                this.Items = new StatusItem[0];
            StatusItem status = new StatusItem();
            Arrays.add(Items, status);
            return status;
        }
        return super.newChildren(name);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Items); }
}

