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
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.network.*;

public class DirectMessagesScreen extends TopScreenBase
    implements NetworkListener, ImageDataListener
{

    private static DirectMessagesScreen _instance = null;

    public static synchronized DirectMessagesScreen getInstance() {
        if (_instance == null)
            _instance = new DirectMessagesScreen();
        return _instance;
    }

    private VarRowHeightListField _list;

    private long _loadReceivedRequestID = -1;
    private long _loadSentRequestID = -1;

    private DirectMessagesScreen() {
        super(TITLE_DIRECT_MESSAGES);

        _list = new VarRowHeightListField() {
            protected boolean onClick(VarRowHeightListField.Item item) {
                return onItemClick(item);
            }
        };
        _list.setEmptyString(getRes().getString(MSG_NO_DIRECT_MESSAGES));

        add(_list);
        setAutoFocusField(_list);
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
        if ((_loadReceivedRequestID >= 0) || (_loadSentRequestID >= 0)) {
            AppUtils.makeTopMenu(menu);
            return;
        }

        VarRowHeightListField.Item item = _list.getSelectedItem();
        MenuItem def = null;
        MenuItem mi;

        if (item != null) {
            if (item instanceof MessageListItem) {
                mi = new MenuItem(getRes(), MENU_OPEN, 1000, 10) {
                    public void run() { showMessage(); }
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

        AppUtils.makeTopMenu(menu);
        if (def != null)
            menu.setDefault(def);
    }

    public void defaultFontChanged() {
        super.defaultFontChanged();
        _list.relayout();
    }

    //

    public void refreshData() {
        if ((_loadReceivedRequestID >= 0) || (_loadSentRequestID > 0))
            return;
        _list.clear();
        _loadReceivedRequestID = TwitterManager.getInstance().directMessages();
        _loadSentRequestID = TwitterManager.getInstance().directMessagesSent();
        showProgress(getRes().getString(MSG_LOADING_DIRECT_MESSAGES));
    }

    private void loadMore() {
        long maxID = -1;

        for (int i = _list.getSize() - 1; i >= 0; i--) {
            VarRowHeightListField.Item item = _list.getItem(i);
            if (item instanceof MessageListItem) {
                maxID = ((MessageListItem)item).getItemID() - 1;
                break;
            }
        }

        if (maxID < 0)
            _loadReceivedRequestID = TwitterManager.getInstance().directMessages();
        else
            _loadSentRequestID = TwitterManager.getInstance().directMessages(maxID);

        showProgress(getRes().getString(MSG_LOADING_DIRECT_MESSAGES));
    }

    private void addMessages(DirectMessageItem[] messages) {
        if ((messages == null) || (messages.length == 0))
            return;

        DirectMessageItem selected = null;
        /*
        VarRowHeightListField.Item selectedItem = _list.getSelectedItem();
        if ((selectedItem != null) && (selectedItem instanceof StatusMessageListItem))
            selected = (DirectMessageItem)(((StatusMessageListItem)selectedItem).getTag());
        */

        SimpleSortingVector itemsList = new SimpleSortingVector();
        itemsList.setSortComparator(new Comparator() {
            public int compare(Object o1, Object o2) {
                long res = ((DirectMessageItem)o2).CreatedAt - ((DirectMessageItem)o1).CreatedAt;
                return (res > 0) ? 1 : ((res < 0) ? -1 : 0);
            }
        });
        itemsList.setSort(true);
        for (int i = _list.getSize() - 1; i >= 0; i--)
            itemsList.add(_list.getItem(i).getTag());
        for (int i = messages.length - 1; i >= 0; i--)
            itemsList.add(messages[i]);

        int selectedIndex = (selected != null) ? itemsList.indexOf(selected) : -1;

        VarRowHeightListField.Item[] items = new VarRowHeightListField.Item[itemsList.size()];
        for (int i = itemsList.size() - 1; i >= 0; i--)
            items[i] = new MessageListItem((DirectMessageItem)itemsList.elementAt(i));
        _list.set(items);
        if (selectedIndex >= 0)
            _list.setSelectedIndex(selectedIndex);
        else if (_list.getSize() > 0)
            _list.setSelectedIndex(0);
    }

    private boolean onItemClick(VarRowHeightListField.Item item) {
        if ((_loadReceivedRequestID >= 0) || (_loadSentRequestID >= 0))
            return true;
        if (item instanceof LoadMoreListItem)
            loadMore();
        else if (item instanceof MessageListItem)
            showMessage();
        return true;
    }

    private void showMessage() {
        VarRowHeightListField.Item item = _list.getSelectedItem();
        if ((item == null) || (!(item instanceof MessageListItem)))
            return;
        new ViewMessageScreen().show(((MessageListItem)item).getDirectMessage());
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loadReceivedRequestID)
            _loadReceivedRequestID = -1;
        else if (id == _loadSentRequestID)
            _loadSentRequestID = -1;
        else
            return;
        if ((_loadReceivedRequestID < 0) && (_loadSentRequestID < 0))
            hideProgress();
        if (result instanceof DirectMessagesItem)
            _app.invokeLater(new RunnableImpl(result) { public void run() {
                addMessages(((DirectMessagesItem)data0).Items);
            }});
    }

    public void error(long id, Throwable ex) {
        if (id == _loadReceivedRequestID)
            _loadReceivedRequestID = -1;
        else if (id == _loadSentRequestID)
            _loadSentRequestID = -1;
        else
            return;
        if ((_loadReceivedRequestID < 0) && (_loadSentRequestID < 0))
            hideProgress();
        UiUtils.alert(ex);
    }

    public void cancelled(long id) {
        if (id == _loadReceivedRequestID)
            _loadReceivedRequestID = -1;
        else if (id == _loadSentRequestID)
            _loadSentRequestID = -1;
        else
            return;
        if ((_loadReceivedRequestID < 0) && (_loadSentRequestID < 0))
            hideProgress();
    }

    // ImageDataListener

    public void imageLoaded(long requestID, String url, EncodedImage image) {
        _app.invokeLater(new Runnable() {
            public void run() { _list.invalidate(); }
        });
    }
}

