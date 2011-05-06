package codeminders.yfrog.app.forms;

import java.util.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;
import net.rim.blackberry.api.invoke.*;
import net.rim.blackberry.api.mail.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.network.*;

public class ViewMessageScreen extends ScreenBase
    implements NetworkListener
{

    private static final String YFROG_LINK_URL = "http://yfrog.";

    private StatusItem _status = null;
    private DirectMessageItem _message = null;
    private SearchResultItem _searchResult = null;
    private Object[] _usersReferences = new Object[0];

    private Vector _images = new Vector();
    private long _deleteRequestID = -1;

    public ViewMessageScreen() {
        super();
    }

    public void show(StatusItem status) {
        _status = status;
        show();
    }

    public void show(DirectMessageItem message) {
        _message = message;
        show();
    }

    public void show(SearchResultItem searchResult) {
        _searchResult = searchResult;
        show();
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
    }

    protected void afterShow(boolean firstShow) {
        UserItemBase user = getItemUser();
        String screenName = getItemScreenName();

        Vector users = new Vector();

        users.addElement((user != null) ? (Object)user : (Object)screenName);

        String[] messageUsers = TwitterManager.getUsersReferences(getItemText());
        for (int i = 0; i < messageUsers.length; i++)
            if (!StringUtilities.strEqual(screenName, messageUsers[i]))
                users.addElement(messageUsers[i]);

        _usersReferences = new Object[users.size()];
        users.copyInto(_usersReferences);

        showData();
    }

    protected void makeFormMenu(Menu menu) {
        if (
            (_status != null)
            || (_searchResult != null)
            || ((_message != null) && (!Options.getInstance().isMyself(_message.SenderScreenName)))
        ) {
            menu.add(new MenuItem(getRes(), MENU_RETWEET, 1000, 10) {
                public void run() { retweetItem(); }
            });
            menu.add(new MenuItem(getRes(), MENU_REPLY, 1000, 10) {
                public void run() { replyItem(); }
            });
            menu.add(new MenuItem(getRes(), MENU_FORWARD_ITEM, 1000, 10) {
                public void run() { forwardItem(); }
            });
        }
        if (
            ((_status != null) && Options.getInstance().isMyself(_status.User.ScreenName))
            || ((_message != null) && Options.getInstance().isMyself(_message.RecipientScreenName))
        )
            menu.add(new MenuItem(getRes(), MENU_DELETE, 1000, 10) {
                public void run() { deleteItem(); }
            });

        GeoItem geo = getItemGeo();
        if ((geo != null) && (!StringUtils.isNullOrEmpty(geo.Point)))
            menu.add(new MenuItem(getRes(), MENU_VIEW_LOCATION, 1000, 10) {
                public void run() { viewLocation(); }
            });

        if (_usersReferences.length > 0) {
            if (menu.getSize() > 0)
                menu.addSeparator();
            for (int i = 0; i < _usersReferences.length; i++)
                menu.add(new ViewUserMenuItem(_usersReferences[i]));
        }

        addBackMenuItem(menu, true);
    }

    public void defaultFontChanged() {
        super.defaultFontChanged();
        showData();
    }

    //

    private UserItemBase getItemUser() {
        if (_status != null) return _status.User;
        if (_message != null) return _message.Sender;
        return null;
    }

    private String getItemScreenName() {
        if (_status != null) return _status.User.ScreenName;
        if (_message != null) return _message.SenderScreenName;
        if (_searchResult != null) return _searchResult.FromUser;
        return "";
    }

    private String getItemText() {
        if (_status != null) return _status.Text;
        if (_message != null) return _message.Text;
        if (_searchResult != null) return _searchResult.Text;
        return "";
    }

    private String getItemIcon() {
        if (_status != null) return _status.User.ProfileImageUrl;
        if (_message != null) return _message.Sender.ProfileImageUrl;
        if (_searchResult != null) return _searchResult.ProfileImageUrl;
        return "";
    }

    private long getItemDate() {
        if (_status != null) return _status.CreatedAt;
        if (_message != null) return _message.CreatedAt;
        if (_searchResult != null) return _searchResult.CreatedAt;
        return 0;
    }

    private GeoItem getItemGeo() {
        if (_status != null) return _status.Geo;
        return null;
    }

    private void addTextPart(String s) {
        add(new ActiveRichTextField(s));
    }

    private void showData() {
        deleteAll();
        _images.removeAllElements();

        setTitle(new TitleManager(this,
            getItemIcon(),
            getItemScreenName(),
            getItemDate()
        ));

        String text = getItemText().trim();
        int start = 0;
        while ((start = text.indexOf(YFROG_LINK_URL, start)) >= 0) {
            int textLength = text.length();
            int end = start + YFROG_LINK_URL.length();
            if ((end >= textLength) || CharacterUtilities.isSpaceChar(text.charAt(end))) {
                start = end;
                continue;
            }
            // skip domain
            while ((end < textLength) && (text.charAt(end) != '/'))
                end++;
            if (end < textLength)
                end++;
            // skip id
            while ((end < textLength) && StringUtils.isLetterOrDigitChar(text.charAt(end)))
                end++;
            String url = text.substring(start, end);
            if (!(YFrogConnector.YFROGisimageUrl(url) || YFrogConnector.YFROGisvideoUrl(url))) {
                start = end;
                continue;
            }

            String s = StringUtils.trimRight(text.substring(0, start));
            if (s.length() > 0)
                addTextPart(s);

            ImageField img = new ImageField(url);
            _images.addElement(img);
            add(img);

            text = text.substring(end).trim();
            start = 0;
        }
        if (text.length() > 0)
            addTextPart(text);

        for (Enumeration e = _images.elements(); e.hasMoreElements();)
            ((ImageField)e.nextElement()).requestImage();
    }

    private void forwardItem() {
        Menu menu = new Menu();
        menu.add(new MenuItem(getRes(), MENU_BY_EMAIL, 1000, 10) {
            public void run() { forwardItemByEmail(); }
        });
        menu.add(new MenuItem(getRes(), MENU_BY_SMS, 1000, 10) {
            public void run() { forwardItemBySMS(); }
        });
        menu.show();
    }

    private void forwardItemByEmail() {
        try {
            Message msg = new Message();
            msg.setContent(getItemText());
            MessageArguments args = new MessageArguments(msg);
            Invoke.invokeApplication(Invoke.APP_TYPE_MESSAGES, args);
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
        }
    }

    private void forwardItemBySMS() {
        try {
            TextMessageImpl msg = new TextMessageImpl(getItemText());
            MessageArguments args = new MessageArguments(msg);
            Invoke.invokeApplication(Invoke.APP_TYPE_MESSAGES, args);
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
        }
    }

    private void viewUser() {
        if (_status != null)
            new ViewUserScreen().show(_status.User);
        else if (_message != null)
            new ViewUserScreen().show(_message.Sender);
        else if (_searchResult != null)
            new ViewUserScreen().show(_searchResult.FromUser);
    }

    private void retweetItem() {
        if (_status != null)
            new NewMessageScreen().showRetweet(_status);
        else if (_message != null)
            new NewMessageScreen().showRetweet(_message);
        else if (_searchResult != null)
            new NewMessageScreen().showRetweet(_searchResult);
    }

    private void replyItem() {
        if (_status != null)
            new NewMessageScreen().showReply(_status);
        else if (_message != null)
            new NewMessageScreen().showReply(_message);
        else if (_searchResult != null)
            new NewMessageScreen().showReply(_searchResult);
    }

    private void deleteItem() {
        if (_status != null)
            _deleteRequestID = TwitterManager.getInstance().statusesDestroy(_status.ID);
        else if (_message != null)
            _deleteRequestID = TwitterManager.getInstance().directMessagesDestroy(_message.ID);
        else
            return;
        showProgress(getRes().getString(MSG_DELETING));
    }

    private void viewLocation() {
        GeoItem geo = getItemGeo();
        if ((geo == null) || StringUtils.isNullOrEmpty(geo.Point))
            return;
        double lat, lon;
        try {
            String[] values = StringUtils.split(geo.Point, ' ');
            lat = Double.parseDouble(values[0]);
            lon = Double.parseDouble(values[1]);
        }
        catch (Exception ex) {
            UiUtils.alert(getRes().getString(MSG_INVALID_LOCATION));
            return;
        }
        AppUtils.showLocation(lat, lon);
    }

    // TextMessageImpl

    private static class TextMessageImpl implements javax.wireless.messaging.TextMessage {

        private String _addr = null;
        private String _text = null;

        public TextMessageImpl(String text) {
            _text = text;
        }

        public String getAddress() { return _addr; }
        public Date getTimestamp() { return null; }
        public void setAddress(String addr) { _addr = addr; }
        public String getPayloadText() { return _text; }
        public void setPayloadText(String data) { _text = data; }
    }

    // TitleManager

    private static class TitleManager extends Manager {

        private static final int H_SPACE = 8;
        private static final int V_SPACE = 4;

        private ViewMessageScreen _owner;

        private UserIconField _icon;
        private LabelField _name;
        private LabelField _date;

        public TitleManager(ViewMessageScreen owner,
            String imageUrl, String screenName, long date
        ) {
            super(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);
            _owner = owner;

            _icon = new UserIconField(imageUrl);

            Font f = Font.getDefault();

            _name = new LabelField(screenName);
            _name.setFont(UiUtils.getBoldFont(f));

            _date = new LabelField(DataManager.getInstance().formatDate(date));
            _date.setFont(f.derive(f.getStyle(), f.getHeight() * 2 / 3));

            add(_icon);
            add(_name);
            add(_date);
        }

        protected void sublayout(int width, int height) {
            layoutChild(_icon, _icon.getPreferredWidth(), _icon.getPreferredHeight());
            int textWidth = width - (_icon.getWidth() + (H_SPACE * 3));
            layoutChild(_name, width, height);
            layoutChild(_date, width, height);
            if ((_name.getWidth() + H_SPACE + _date.getWidth()) > textWidth) {
                layoutChild(_date, (textWidth - H_SPACE) / 3, height);
                layoutChild(_name, textWidth - H_SPACE - _date.getWidth(), height);
            }

            setPositionChild(_icon, H_SPACE, V_SPACE);
            setPositionChild(_name, _icon.getWidth() + (H_SPACE * 2), V_SPACE);
            setPositionChild(_date, width - H_SPACE - _date.getWidth(), V_SPACE);

            height = Math.max(_name.getHeight(), _date.getHeight());
            height = Math.max(_icon.getHeight(), height);
            setExtent(width, height + (V_SPACE * 2));
        }

        protected boolean keyChar(char c, int status, int time) {
            switch (c) {
                case Characters.SPACE:
                case Characters.ENTER:
                    onClick();
                    return true;
            }
            return false;
        }

        protected boolean trackwheelClick(int status, int time) {
            onClick();
            return true;
        }

        /* VERSION_DEPENDED: 4.7 * /
        protected boolean touchEvent(TouchEvent message) {
            if (!message.isValid())
                return super.touchEvent(message);
            try {
                switch (message.getEvent()) {
                    case TouchEvent.CLICK:
                        onClick();
                        return true;
                    case TouchEvent.UNCLICK:
                        return true;
                }
            }
            catch (Exception ignored) { }
            return super.touchEvent(message);
        }
        /* */

        private void onClick() {
            _owner.viewUser();
        }
    }

    // ImageField

    private static class ImageField extends BitmapField {

        private String _url;
        private EncodedImage _img = null;
        private long _requestID = -1;

        public ImageField(String url) {
            super(getDefaultImage(url).getBitmap(), FOCUSABLE);
            setSpace(2, 2);
            _url = url;
        }

        private static EncodedImage getDefaultImage(String url) {
            if (YFrogConnector.YFROGisvideoUrl(url))
                return EncodedImage.getEncodedImageResource("video.png");
            else
                return EncodedImage.getEncodedImageResource("image.png");
        }

        public ContextMenu getContextMenu() {
            ContextMenu menu = super.getContextMenu();
            menu.clear();
            MenuItem mi = new MenuItem(ResManager.getResourceBundle(), MENU_OPEN, 1000, 10) {
                public void run() { onClick(); }
            };
            menu.addItem(mi);
            menu.setDefaultItem(mi);
            return menu;
        }

        protected boolean keyChar(char c, int status, int time) {
            switch (c) {
                case Characters.SPACE:
                case Characters.ENTER:
                    onClick();
                    return true;
            }
            return false;
        }

        protected boolean trackwheelClick(int status, int time) {
            onClick();
            return true;
        }

        public void requestImage() {
            String thumbnailUrl = new YFrogConnector(null).YFROGthumbnailUrl(_url);
            if (thumbnailUrl == null)
                return;
            _requestID = NetworkManager.getInstance()
                .startImageRequest(thumbnailUrl);
        }

        public void requestComplete(long id, Object result) {
            if (id != _requestID)
                return;
            _requestID = -1;
            UiApplication.getUiApplication().invokeLater(new RunnableImpl(result) {
                public void run() {
                    _img = (EncodedImage)data0;
                    setImage(_img);
                }
            });
        }

        private void onClick() {
            EncodedImage img = _img;
            if (img == null)
                img = getDefaultImage(_url);

            if (YFrogConnector.YFROGisimageUrl(_url))
                new ViewImageScreen().show(_url, img);
            else if (YFrogConnector.YFROGisvideoUrl(_url)) {
                if (ViewVideoScreen.isPlaybackSupported(_url))
                    new ViewVideoScreen().show(_url, img);
                else
                    UiUtils.alert(ResManager.getString(MSG_UNSUPPORTED_VIDEO));
            }
            else
                UiUtils.alert(ResManager.getString(MSG_UNSUPPORTED_MEDIA));
        }
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (_deleteRequestID == id) {
            _deleteRequestID = -1;
            hideProgress();
            close();
            if (_status != null) {
                HomeListScreen.getInstance().refreshDataIfVisible();
                MentionsScreen.getInstance().refreshDataIfVisible();
            }
            if (_message != null)
                DirectMessagesScreen.getInstance().refreshDataIfVisible();
            return;
        }
        for (Enumeration e = _images.elements(); e.hasMoreElements();)
            ((ImageField)e.nextElement()).requestComplete(id, result);
    }

    public void error(long id, Throwable ex) {
        if (_deleteRequestID == id) {
            _deleteRequestID = -1;
            hideProgress();
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if (_deleteRequestID == id) {
            _deleteRequestID = -1;
            hideProgress();
        }
    }
}

