package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class RelationshipItem extends ItemBase implements Persistable {

    public RelationshipEntryItem Source = null;
    public RelationshipEntryItem Target = null;

    public RelationshipItem() { }

    public ItemBase newChildren(String name) {
        if ("source".equals(name)) {
            if (this.Source == null)
                this.Source = new RelationshipEntryItem();
            return Source;
        }
        if ("target".equals(name)) {
            if (this.Target == null)
                this.Target = new RelationshipEntryItem();
            return Target;
        }
        return super.newChildren(name);
    }

    public String toString() { return "" + Source + "\n" + Target; }
}

