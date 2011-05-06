package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Arrays;
import net.rim.device.api.util.Persistable;

import codeminders.yfrog.utils.StringUtils;

public class SavedSearchesItem extends ItemBase implements Persistable {

    public SavedSearchItem[] Items = null;

    public SavedSearchesItem() { }

    public ItemBase newChildren(String name) {
        if ("saved_search".equals(name)) {
            if (this.Items == null)
                this.Items = new SavedSearchItem[0];
            SavedSearchItem item = new SavedSearchItem();
            Arrays.add(Items, item);
            return item;
        }
        return super.newChildren(name);
    }

    public String toString() { return StringUtils.arrayToMultilineString(Items); }
}

