package codeminders.yfrog.app.forms;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.data.items.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;

public class UnsentScreen extends TopScreenBase
    implements DataListener, NetworkListener
{

    private static UnsentScreen _instance = null;

    public static synchronized UnsentScreen getInstance() {
        if (_instance == null)
            _instance = new UnsentScreen();
        return _instance;
    }

    private VarRowHeightListField _list;

    private LongHashtable _sendQueue = new LongHashtable();

    private UnsentScreen() {
        super(TITLE_UNSENT);

        _list = new VarRowHeightListField();
        _list.setEmptyString(getRes().getString(MSG_NO_TWEETS));

        add(_list);
        setAutoFocusField(_list);
    }

    protected void onDisplay() {
        DataManager.getInstance().addUnsentListener(this);
        DataManager.getInstance().addSendListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        DataManager.getInstance().removeUnsentListener(this);
        DataManager.getInstance().removeSendListener(this);
    }

    protected void makeFormMenu(Menu menu) {
        if (requestsActive()) {
            AppUtils.makeTopMenu(menu);
            return;
        }

        NameTextListItem item = (NameTextListItem)_list.getSelectedItem();
        MenuItem def = null;
        MenuItem mi;

        if (item != null) {
            mi = new MenuItem(getRes(), MENU_SEND, 1000, 10) {
                public void run() { sendItem(); }
            };
            menu.add(mi);
            def = mi;
        }
        if (_list.getSize() > 0) {
            mi = new MenuItem(getRes(), MENU_SEND_ALL, 1000, 10) {
                public void run() { sendAll(); }
            };
            menu.add(mi);
            if (def == null)
                def = mi;
        }
        if (item != null)
            menu.add(mi = new MenuItem(getRes(), MENU_DELETE, 1000, 10) {
                public void run() { deleteItem(); }
            });

        AppUtils.makeTopMenu(menu);
        if (def != null)
            menu.setDefault(def);
    }

    public void defaultFontChanged() {
        super.defaultFontChanged();
        _list.relayout();
    }

    //

    private boolean requestsActive() {
        synchronized (_sendQueue) {
            return (!_sendQueue.isEmpty());
        }
    }

    public void refreshData() {
        refreshData(null);
    }

    private void refreshData(UnsentItemBase selected) {
        _list.clear();

        int selectedIndex = -1;
        UnsentItemBase[] items = DataManager.getInstance().getUnsentItems();
        NameTextListItem[] listItems = new NameTextListItem[items.length];
        for (int i = items.length - 1, j = 0; i >= 0; i--, j++) {
            if (items[i] == selected)
                selectedIndex = j;
            UnsentItemBase item = items[i];
            if (item instanceof UnsentDirectMessageItem)
                listItems[j] = new NameTextListItem(((UnsentDirectMessageItem)item).RecipientScreenName, item);
            else
                listItems[j] = new NameTextListItem(item);
        }
        _list.set(listItems);
        if (selectedIndex >= 0)
            _list.setSelectedIndex(selectedIndex);
        else if (_list.getSize() > 0)
            _list.setSelectedIndex(0);
    }

    //

    private void sendItem() {
        NameTextListItem item = (NameTextListItem)_list.getSelectedItem();
        if (item == null)
            return;
        sendItem((UnsentItemBase)item.getTag(), true);
    }

    private void sendAll() {
        VarRowHeightListField.Item[] items = _list.getItems();
        for (int i = 0; i < items.length; i++)
            sendItem((UnsentItemBase)((NameTextListItem)items[i]).getTag(), false);
    }

    private void sendItem(UnsentItemBase item, boolean showError) {
        if (item == null)
            return;
        if (!Options.getInstance().isMyself(item.SenderScreenName))
            return;
        try {
            synchronized (_sendQueue) {
                long id = DataManager.getInstance().send(item);
                _sendQueue.put(id, item);
            }
        }
        catch (Exception ex) {
            if (showError)
                UiUtils.alert(ex);
            return;
        }
        showProgress(getRes().getString(MSG_SENDING));
    }

    private void deleteItem() {
        NameTextListItem item = (NameTextListItem)_list.getSelectedItem();
        if (item == null)
            return;
        DataManager.getInstance().removeUnsentItem((UnsentItemBase)item.getTag());
    }

    private void tryHideProgress() {
        if (requestsActive())
            return;
        hideProgress();
        HomeListScreen.getInstance().refreshDataIfVisible();
        MentionsScreen.getInstance().refreshDataIfVisible();
        DirectMessagesScreen.getInstance().refreshDataIfVisible();
    }

    // DataListener

    public void itemAdded(Object item) {
        _app.invokeLater(new RunnableImpl(item) { public void run() {
            refreshData((UnsentItemBase)data0);
        }});
    }

    public void itemRemoved(Object item) {
        _app.invokeLater(new Runnable() { public void run() {
            refreshData();
        }});
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        UnsentItemBase item = null;
        synchronized (_sendQueue) {
            item = (UnsentItemBase)_sendQueue.get(id);
            if (item == null)
                return;
            _sendQueue.remove(id);
        }
        DataManager.getInstance().removeUnsentItem(item);
        tryHideProgress();
    }

    public void error(long id, Throwable ex) {
        synchronized (_sendQueue) {
            if (!_sendQueue.containsKey(id))
                return;
            _sendQueue.remove(id);
        }
        tryHideProgress();
        UiUtils.alert(ex);
    }

    public void cancelled(long id) {
        synchronized (_sendQueue) {
            if (!_sendQueue.containsKey(id))
                return;
            _sendQueue.remove(id);
        }
        tryHideProgress();
    }
}

