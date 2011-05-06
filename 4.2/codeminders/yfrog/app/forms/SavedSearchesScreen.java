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

public class SavedSearchesScreen extends ScreenBase
    implements NetworkListener
{

    private VarRowHeightListField _list;

    private long _loadRequestID = -1;
    private long _deleteRequestID = -1;

    public SavedSearchesScreen() {
        super(TITLE_SAVED_SEARCHES);

        _list = new VarRowHeightListField() {
            protected boolean onClick(VarRowHeightListField.Item item) {
                return onItemClick(item);
            }
        };
        _list.setEmptyString(getRes().getString(MSG_NO_SAVED_SEARCHES));

        add(_list);
        setAutoFocusField(_list);
    }

    protected void afterShow(boolean firstShow) {
        refreshData();
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
    }

    protected void makeFormMenu(Menu menu) {
        if ((_loadRequestID >= 0) || (_deleteRequestID >= 0)) {
            addBackMenuItem(menu, true);
            return;
        }

        VarRowHeightListField.Item item = _list.getSelectedItem();
        MenuItem def = null;
        MenuItem mi;

        if (item != null) {
            mi = new MenuItem(getRes(), MENU_SHOW_SEARCH, 1000, 10) {
                public void run() { showSearch(); }
            };
            menu.add(mi);
            def = mi;

            menu.add(new MenuItem(getRes(), MENU_DELETE, 1000, 10) {
                public void run() { deleteSearch(); }
            });
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
        _loadRequestID = TwitterManager.getInstance().savedSearches();
        showProgress(getRes().getString(MSG_LOADING_SAVED_SEARCHES));
    }

    private void addSearches(SavedSearchItem[] searches) {
        VarRowHeightListField.Item[] items;

        if ((searches == null) || (searches.length == 0)) {
            items = new VarRowHeightListField.Item[0];
        }
        else {
            int itemsCount = searches.length;
            items = new VarRowHeightListField.Item[itemsCount];
            for (int i = 0; i < itemsCount; i++) {
                SavedSearchItem item = searches[i];
                items[i] = new NameTextListItem(null, item.Query, item);
                //items[i] = new NameTextListItem(item.Name, item.Query, item);
            }
        }

        VarRowHeightListField.Item lastItem = _list.getItem(_list.getSize() - 1);
        _list.add(items);
    }

    private boolean onItemClick(VarRowHeightListField.Item item) {
        if ((_loadRequestID >= 0) || (_deleteRequestID >= 0))
            return true;
        showSearch();
        return true;
    }

    private void showSearch() {
        VarRowHeightListField.Item item = _list.getSelectedItem();
        if ((item == null) || (!(item instanceof NameTextListItem)))
            return;
        SavedSearchItem searchItem = (SavedSearchItem)((NameTextListItem)item).getTag();
        SearchScreen.getInstance().show(searchItem);
        close();
    }

    private void deleteSearch() {
        VarRowHeightListField.Item item = _list.getSelectedItem();
        if ((item == null) || (!(item instanceof NameTextListItem)))
            return;
        SavedSearchItem searchItem = (SavedSearchItem)((NameTextListItem)item).getTag();
        _deleteRequestID = TwitterManager.getInstance().savedSearchesDestroy(searchItem.ID);
        showProgress(getRes().getString(MSG_DELETING));
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if ((id == _loadRequestID) || (id == _deleteRequestID)) {
            hideProgress();
            if (id == _loadRequestID) {
                _loadRequestID = -1;
                if (result instanceof SavedSearchesItem)
                    _app.invokeLater(new RunnableImpl(result) { public void run() {
                        addSearches(((SavedSearchesItem)data0).Items);
                    }});
            }
            else if (id == _deleteRequestID) {
                _deleteRequestID = -1;
                _app.invokeLater(new RunnableImpl() { public void run() {
                    refreshData();
                }});
            }
        }
    }

    public void error(long id, Throwable ex) {
        if ((id == _loadRequestID) || (id == _deleteRequestID)) {
            hideProgress();
            if (id == _loadRequestID)
                _loadRequestID = -1;
            else if (id == _deleteRequestID)
                _deleteRequestID = -1;
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if ((id == _loadRequestID) || (id == _deleteRequestID)) {
            hideProgress();
            if (id == _loadRequestID)
                _loadRequestID = -1;
            else if (id == _deleteRequestID)
                _deleteRequestID = -1;
        }
    }
}

