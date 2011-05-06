package codeminders.yfrog.twitter.items;

import net.rim.device.api.util.Persistable;

public class UserItemBase extends ItemBase implements Persistable {

    public long ID = 0;
    public String Name = null;
    public String ScreenName = null;
    public String Location = null;
    public String Description = null;
    public String ProfileImageUrl = null;
    public String Url = null;
    public boolean Protected = false;
    public long FollowersCount = 0;
    public int ProfileBackgroundColor = 0;
    public int ProfileTextColor = 0;
    public int ProfileLinkColor = 0;
    public int ProfileSidebarFillColor = 0;
    public int ProfileSidebarBorderColor = 0;
    public long FriendsCount = 0;
    public long CreatedAt = 0;
    public long FavouritesCount = 0;
    public long UtcOffset = 0;
    public String TimeZone = null;
    public String ProfileBackgroundImageUrl = null;
    public boolean ProfileBackgroundTile = false;
    public int StatusesCount = 0;
    public boolean Verified = false;
    public boolean Notifications = false;
    public boolean Following = false;
    public boolean GeoEnabled = false;

    public UserItemBase() { }

    public void setField(String name, String value) {
        if ("id".equals(name)) this.ID = Long.parseLong(value);
        else if ("name".equals(name)) this.Name = value;
        else if ("screen_name".equals(name)) this.ScreenName = value;
        else if ("location".equals(name)) this.Location = value;
        else if ("description".equals(name)) this.Description = value;
        else if ("profile_image_url".equals(name)) this.ProfileImageUrl = value;
        else if ("url".equals(name)) this.Url = value;
        else if ("protected".equals(name)) this.Protected = "true".equals(value);
        else if ("followers_count".equals(name)) this.FollowersCount = Long.parseLong(value);
        else if ("profile_background_color".equals(name)) this.ProfileBackgroundColor = Integer.parseInt(value, 16);
        else if ("profile_text_color".equals(name)) this.ProfileTextColor = Integer.parseInt(value, 16);
        else if ("profile_link_color".equals(name)) this.ProfileLinkColor = Integer.parseInt(value, 16);
        else if ("profile_sidebar_fill_color".equals(name)) this.ProfileSidebarFillColor = Integer.parseInt(value, 16);
        else if ("profile_sidebar_border_color".equals(name)) this.ProfileSidebarBorderColor = Integer.parseInt(value, 16);
        else if ("friends_count".equals(name)) this.FriendsCount = Long.parseLong(value);
        else if ("created_at".equals(name)) this.CreatedAt = parseDate(value);
        else if ("favourites_count".equals(name)) this.FavouritesCount = Long.parseLong(value);
        else if ("utc_offset".equals(name)) this.UtcOffset = Long.parseLong(value);
        else if ("time_zone".equals(name)) this.TimeZone = value;
        else if ("profile_background_image_url".equals(name)) this.ProfileBackgroundImageUrl = value;
        else if ("profile_background_tile".equals(name)) this.ProfileBackgroundTile = "true".equals(value);
        else if ("statuses_count".equals(name)) this.StatusesCount = Integer.parseInt(value);
        else if ("verified".equals(name)) this.Verified = "true".equals(value);
        else if ("notifications".equals(name)) this.Notifications = "true".equals(value);
        else if ("following".equals(name)) this.Following = "true".equals(value);
        else if ("geo_enabled".equals(name)) this.GeoEnabled = "true".equals(value);
        else super.setField(name, value);
    }

    public String toString() { return Name; }
}

