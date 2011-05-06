package codeminders.yfrog.app.forms;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.network.*;

public class SearchScreen extends TopScreenBase
    implements NetworkListener, ImageDataListener
{

    private static SearchScreen _instance = null;

    public static synchronized SearchScreen getInstance() {
        if (_instance == null)
            _instance = new SearchScreen();
        return _instance;
    }

    private SearchField _search;
    private VarRowHeightListField _list;

    private SavedSearchItem _savedSearch = null;
    private long _searchRequestID = -1;
    private String _searchText = null;
    private int _page = -1;
    private long _saveRequestID = -1;

    private SearchScreen() {
        super();

        _search = new SearchField(LABEL_SEARCH) {
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

    public void show(SavedSearchItem savedSearch) {
        _savedSearch = savedSearch;
        _search.setText(savedSearch.Query);
        show();
        runSearch();
        UiUtils.setFocus(_search.getEdit());
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
        if ((_searchRequestID >= 0) || (_saveRequestID >= 0)) {
            AppUtils.makeTopMenu(menu);
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
            if (item instanceof MessageListItem) {
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

        if (hasText)
            menu.add(new MenuItem(getRes(), MSG_SAVE_SEARCH, 1000, 10) {
                public void run() { saveSearch(); }
            });

        menu.add(new MenuItem(getRes(), MENU_SAVED_SEARCHES, 1000, 10) {
            public void run() { showSaved(); }
        });
        menu.add(new MenuItem(getRes(), MENU_SEARCH_USER, 1000, 10) {
            public void run() { searchUser(); }
        });

        AppUtils.makeTopMenu(menu);
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
        if ((_searchRequestID >= 0) || (_saveRequestID >= 0))
            return true;
        if (item == null)
            return true;
        if (item instanceof LoadMoreListItem)
            loadMore();
        else if (item instanceof MessageListItem)
            new ViewMessageScreen().show(((MessageListItem)item).getSearchResult());
        return true;
    }

    private void saveSearch() {
        String text = _search.getText();
        if (text.length() == 0)
            return;
        _saveRequestID = TwitterManager.getInstance().savedSearchesCreate(text);
        showProgress(getRes().getString(MSG_SAVING));
    }

    private void showSaved() {
        new SavedSearchesScreen().show();
    }

    private void searchUser() {
        new SearchUserScreen().show();
    }

    private void runSearch() {
        if ((_searchRequestID >= 0) || (_saveRequestID >= 0))
            return;

        String text = _search.getText();
        if (text.length() == 0)
            return;
        _page = -1;
        _searchText = text;
        _list.setEmptyString("");
        _list.clear();
        _searchRequestID = TwitterManager.getInstance().search(_searchText);
        showProgress(getRes().getString(MSG_SEARCHING));
    }

    private void loadMore() {
        if (_searchText == null)
            return;

        long maxID = -1;

        for (int i = _list.getSize() - 1; i >= 0; i--) {
            VarRowHeightListField.Item item = _list.getItem(i);
            if (item instanceof MessageListItem) {
                maxID = ((MessageListItem)item).getItemID() - 1;
                break;
            }
        }

        if ((maxID < 0) || (_page < 0))
            _searchRequestID = TwitterManager.getInstance().search(_searchText);
        else
            _searchRequestID = TwitterManager.getInstance().search(_searchText, _page + 1, maxID);

        showProgress(getRes().getString(MSG_SEARCHING));
    }

    private void addResults(SearchResultItem[] results, int page) {
        boolean firstPage = (_page < 0);
        _page = page;
        VarRowHeightListField.Item[] items;

        if ((results == null) || (results.length == 0)) {
            items = new VarRowHeightListField.Item[0];
            if (firstPage)
                _list.setEmptyString(getRes().getString(MSG_NO_TWEETS_FOUND));
        }
        else {
            int itemsCount = results.length;
            items = new VarRowHeightListField.Item[itemsCount];
            for (int i = 0; i < itemsCount; i++)
                items[i] = new MessageListItem(results[i]);
            if (itemsCount >= TwitterManager.STATUSES_PAGE_SIZE)
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
            if (result instanceof SearchResultsItem)
                _app.invokeLater(new RunnableImpl(result) { public void run() {
                    addResults(((SearchResultsItem)data0).Results, ((SearchResultsItem)data0).Page);
                }});
        }
        else if (id == _saveRequestID) {
            _saveRequestID = -1;
            hideProgress();
        }
    }

    public void error(long id, Throwable ex) {
        if ((id == _searchRequestID) || (id == _saveRequestID)) {
            if (id == _searchRequestID)
                _searchRequestID = -1;
            else if (id == _saveRequestID)
                _saveRequestID = -1;
            hideProgress();
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if ((id == _searchRequestID) || (id == _saveRequestID)) {
            if (id == _searchRequestID)
                _searchRequestID = -1;
            else if (id == _saveRequestID)
                _saveRequestID = -1;
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

