package codeminders.yfrog.app.forms;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.network.*;

public class UserFollowersScreen extends ScreenBase
    implements NetworkListener, ImageDataListener
{

    private VarRowHeightListField _list;

    private String _userScreenName;

    private long _loadRequestID = -1;
    private long _nextCursorID = 0;

    public UserFollowersScreen() {
        super();

        _list = new VarRowHeightListField() {
            protected boolean onClick(VarRowHeightListField.Item item) {
                return onItemClick(item);
            }
        };
        _list.setEmptyString(getRes().getString(MSG_NO_FOLLOWERS));

        add(_list);
        setAutoFocusField(_list);
    }

    public void show(String screenName) {
        _userScreenName = screenName;
        setTitle(_userScreenName);
        show();
    }

    protected void afterShow(boolean firstShow) {
        refreshData();
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        DataManager.getInstance().addIconListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
        DataManager.getInstance().removeIconListener(this);
    }

    protected void makeFormMenu(Menu menu) {
        if (_loadRequestID >= 0) {
            addBackMenuItem(menu, true);
            return;
        }

        VarRowHeightListField.Item item = _list.getSelectedItem();
        MenuItem def = null;
        MenuItem mi;

        if (item != null) {
            if (item instanceof UserListItem) {
                mi = new MenuItem(getRes(), MENU_OPEN, 1000, 10) {
                    public void run() { showUser(); }
                };
                menu.add(mi);
                def = mi;
            }
            else if (item instanceof LoadMoreListItem) {
                mi = new MenuItem(getRes(), MENU_LOAD_MORE, 1000, 10) {
                    public void run() { loadMore(); }
                };
                menu.add(mi);
                def = mi;
            }
        }

        mi = new MenuItem(getRes(), MENU_REFRESH, 1000, 10) {
            public void run() { refreshData(); }
        };
        menu.add(mi);
        if (def == null)
            def = mi;

        addBackMenuItem(menu, false);

        if (def != null)
            menu.setDefault(def);
    }

    public void defaultFontChanged() {
        super.defaultFontChanged();
        _list.relayout();
    }

    //

    public void refreshData() {
        _list.clear();
        _loadRequestID = TwitterManager.getInstance().statusesFollowers(
            _userScreenName);
        showProgress(getRes().getString(MSG_LOADING_FOLLOWERS));
    }

    private void loadMore() {
        if (_nextCursorID == 0)
            _loadRequestID = TwitterManager.getInstance().statusesFollowers(
                _userScreenName);
        else
            _loadRequestID = TwitterManager.getInstance().statusesFollowers(
                _userScreenName, _nextCursorID);
        showProgress(getRes().getString(MSG_LOADING_FOLLOWERS));
    }

    private void addUsers(UserItem[] users, long nextCursorID) {
        _nextCursorID = nextCursorID;
        VarRowHeightListField.Item[] items;

        if ((users == null) || (users.length == 0)) {
            items = new VarRowHeightListField.Item[0];
        }
        else {
            int itemsCount = users.length;
            items = new VarRowHeightListField.Item[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                items[i] = new UserListItem(users[i]);
            if (_nextCursorID != 0)
                Arrays.add(items, new LoadMoreListItem());
        }

        VarRowHeightListField.Item lastItem = _list.getItem(_list.getSize() - 1);
        boolean isLoadMorePresent = (lastItem != null) && (lastItem instanceof LoadMoreListItem);
        _list.removeLastAdd(
            isLoadMorePresent ? 1 : 0,
            items
        );
    }

    private boolean onItemClick(VarRowHeightListField.Item item) {
        if (_loadRequestID >= 0)
            return true;
        if (item instanceof LoadMoreListItem)
            loadMore();
        else if (item instanceof UserListItem)
            showUser();
        return true;
    }

    private void showUser() {
        VarRowHeightListField.Item item = _list.getSelectedItem();
        if ((item == null) || (!(item instanceof UserListItem)))
            return;
        new ViewUserScreen().show(((UserListItem)item).getUser());
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id != _loadRequestID)
            return;
        _loadRequestID = -1;
        hideProgress();
        if (result instanceof UsersListItem)
            _app.invokeLater(new RunnableImpl(result) { public void run() {
                addUsers(((UsersListItem)data0).Users.Items, ((UsersListItem)data0).NextCursor);
            }});
        else if (result instanceof UsersItem)
            _app.invokeLater(new RunnableImpl(result) { public void run() {
                addUsers(((UsersItem)data0).Items, 0);
            }});
    }

    public void error(long id, Throwable ex) {
        if (id != _loadRequestID)
            return;
        _loadRequestID = -1;
        hideProgress();
        UiUtils.alert(ex);
    }

    public void cancelled(long id) {
        if (id != _loadRequestID)
            return;
        _loadRequestID = -1;
        hideProgress();
    }

    // ImageDataListener

    public void imageLoaded(long requestID, String url, EncodedImage image) {
        _app.invokeLater(new Runnable() {
            public void run() { _list.invalidate(); }
        });
    }
}

