package codeminders.yfrog.app.controls;

import net.rim.device.api.ui.MenuItem;

import codeminders.yfrog.res.*;
import codeminders.yfrog.twitter.items.UserItemBase;
import codeminders.yfrog.app.forms.ViewUserScreen;

public class ViewUserMenuItem extends MenuItem implements YFrogResource {

    private Object _user;

    public ViewUserMenuItem(Object user) {
        super(ResManager.getString(MENU_VIEW_USER_FMT, new Object[] { getName(user) }), 1000, 10);
        _user = user;
    }

    private static String getName(Object user) {
        if (user instanceof UserItemBase)
            return ((UserItemBase)user).ScreenName;
        else
            return user.toString();
    }

    public void run() {
        if (_user instanceof UserItemBase)
            new ViewUserScreen().show((UserItemBase)_user);
        else
            new ViewUserScreen().show(_user.toString());
    }
}

