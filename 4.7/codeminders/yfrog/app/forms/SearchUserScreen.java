package codeminders.yfrog.app.forms;

import javax.microedition.io.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.app.controls.*;

public class SearchUserScreen extends ScreenBase
    implements NetworkListener, ImageDataListener
{

    private SearchField _search;
    private VarRowHeightListField _list;

    private long _searchRequestID = -1;
    private String _searchText = null;
    private int _page = -1;

    public SearchUserScreen() {
        super();

        _search = new SearchField(LABEL_SEARCH_USER) {
            protected void onSearch() { runSearch(); }
        };

        _list = new VarRowHeightListField() {
            protected boolean onClick(VarRowHeightListField.Item item) {
                return onItemClick(item);
            }
        };

        setTitle(_search);
        add(_list);
        setAutoFocusField(_search.getEdit());
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
        if (_searchRequestID >= 0) {
            addBackMenuItem(menu, true);
            return;
        }

        boolean hasText = (_search.getText().length() > 0);
        VarRowHeightListField.Item item = _list.getSelectedItem();
        MenuItem def = null;
        MenuItem mi;

        if (hasText) {
            mi = new MenuItem(getRes(), MENU_GO, 1000, 10) {
                public void run() { runSearch(); }
            };
            menu.add(mi);
            if (getLeafFieldWithFocus() == _search.getEdit())
                def = mi;
        }

        if ((item != null) && (getLeafFieldWithFocus() == _list)) {
            if (item instanceof UserListItem) {
                mi = new MenuItem(getRes(), MENU_OPEN, 1000, 10) {
                    public void run() { openItem(); }
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

        addBackMenuItem(menu, false);
        if (def != null)
            menu.setDefault(def);
    }

    protected boolean keyChar(char c, int status, int time) {
        if ((getLeafFieldWithFocus() == _list) && StringUtils.isTextChar(c))
            _search.getEdit().setFocus();
        return super.keyChar(c, status, time);
    }

    //

    private void openItem() {
        onItemClick(_list.getSelectedItem());
    }

    private boolean onItemClick(VarRowHeightListField.Item item) {
        if (_searchRequestID >= 0)
            return true;
        if (item == null)
            return true;
        if (item instanceof LoadMoreListItem)
            loadMore();
        else if (item instanceof UserListItem)
            new ViewUserScreen().show(((UserListItem)item).getUser());
        return true;
    }

    private void runSearch() {
        if (_searchRequestID >= 0)
            return;

        String text = _search.getText();
        if (text.length() == 0)
            return;
        _page = -1;
        _searchText = text;
        _list.setEmptyString("");
        _list.clear();
        _searchRequestID = TwitterManager.getInstance().usersShow(_searchText);
        showProgress(getRes().getString(MSG_SEARCHING));
    }

    private void loadMore() {
    }

    private void addResults(UserItemBase[] results, int page) {
        boolean firstPage = (_page < 0);
        _page = page;
        VarRowHeightListField.Item[] items;

        if ((results == null) || (results.length == 0)) {
            items = new VarRowHeightListField.Item[0];
            if (firstPage)
                _list.setEmptyString(getRes().getString(MSG_NO_USERS_FOUND));
        }
        else {
            int itemsCount = results.length;
            items = new VarRowHeightListField.Item[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                items[i] = new UserListItem((UserItemBase)results[i]);
            if (itemsCount >= TwitterManager.USERS_PAGE_SIZE)
               Arrays.add(items, new LoadMoreListItem());
        }

        VarRowHeightListField.Item lastItem = _list.getItem(_list.getSize() - 1);
        boolean isLoadMorePresent = (lastItem != null) && (lastItem instanceof LoadMoreListItem);
        _list.removeLastAdd(
            isLoadMorePresent ? 1 : 0,
            items
        );
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _searchRequestID) {
            _searchRequestID = -1;
            hideProgress();
            if (result instanceof UserItemBase)
                _app.invokeLater(new RunnableImpl(result) { public void run() {
                    addResults(new UserItemBase[] { (UserItemBase)data0 }, 1);
                }});
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _searchRequestID) {
            _searchRequestID = -1;
            hideProgress();
            if (
                (ex instanceof NetworkException)
                && (((NetworkException)ex).getResponseCode() == HttpConnection.HTTP_NOT_FOUND)
            ) {
                _app.invokeLater(new Runnable() { public void run() {
                    addResults(null, 1);
                }});
                return;
            }
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if (id == _searchRequestID) {
            _searchRequestID = -1;
            hideProgress();
        }
    }

    // ImageDataListener

    public void imageLoaded(long requestID, String url, EncodedImage image) {
        _app.invokeLater(new Runnable() {
            public void run() { _list.invalidate(); }
        });
    }
}

