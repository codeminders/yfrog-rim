package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class SearchResultItem extends JsonItemBase implements Persistable {

    public long ID = 0;
    public long CreatedAt = 0;
    public String FromUser = null;
    public long FromUserID = 0;
    public String ProfileImageUrl = null;
    public long ToUserID = 0;
    public String Text = null;
    public String IsoLanguageCode = null;
    public String Source = null;

    public SearchResultItem() { }

    protected void setField(String name, Object value) {
        if ("id".equals(name)) this.ID = Long.parseLong(value.toString());
        else if ("created_at".equals(name)) this.CreatedAt = ItemBase.parseDate(value.toString());
        else if ("from_user".equals(name)) this.FromUser = value.toString();
        else if ("from_user_id".equals(name)) this.FromUserID = Long.parseLong(value.toString());
        else if ("profile_image_url".equals(name)) this.ProfileImageUrl = value.toString();
        else if ("to_user_id".equals(name)) this.ToUserID = Long.parseLong(value.toString());
        else if ("text".equals(name)) this.Text = ItemBase.parseText(value.toString());
        else if ("iso_language_code".equals(name)) this.IsoLanguageCode = value.toString();
        else if ("source".equals(name)) this.Source = ItemBase.parseText(value.toString());
        else super.setField(name, value);
    }

    public String toString() { return Text; }
}

