package codeminders.yfrog.app.forms;

import java.util.*;
import javax.microedition.io.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;

public class ViewUserScreen extends ScreenBase
    implements NetworkListener
{

    private String _userScreenName;
    private UserItemBase _user = null;
    private UserLocation _userLocation = null;
    private boolean _isUserMyself = false;
    private UserTitleManager _userTitle;
    private ActiveRichTextField _description;
    private ButtonField _btnFollowers, _btnViewLocation;

    private Manager _boxNotificationsManager;
    private CheckboxField _boxNotifications;

    private Manager _btnDirectMessageManager;
    private ButtonField _btnDirectMessage;

    private long _loadUserRequestID = -1;
    private long _loadRelationshipRequestID = -1;
    private long _updateFollowRequestID = -1;
    private long _updateNotificationsRequestID = -1;

    private boolean _relationshipReceived = false;
    private RelationshipItem _relationship = null;

    public ViewUserScreen() {
        super();
        disableDefaultMenuItems();

        _userTitle = new UserTitleManager();
        _description = new ActiveRichTextField("");

        _boxNotificationsManager = new VerticalFieldManager(
            NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL | USE_ALL_WIDTH);
        _btnDirectMessageManager = new VerticalFieldManager(
            NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL | USE_ALL_WIDTH);

        _boxNotifications = new CheckboxField(getRes().getString(LABEL_USER_NOTIFICATIONS), false);
        _boxNotifications.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (context == FieldChangeListener.PROGRAMMATIC)
                    return;
                _app.invokeLater(new Runnable() { public void run() {
                    sendNotifications();
                }});
            }
        });

        ButtonField btnReply = new ButtonField(getRes().getString(MENU_SEND_PUBLIC_REPLY),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        btnReply.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                sendReply();
            }
        });

        ButtonField btnTweets = new ButtonField(getRes().getString(MENU_RECENT_TWEETS),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        btnTweets.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                recentTweets();
            }
        });

        _btnFollowers = new ButtonField(getRes().getString(MENU_USER_FOLLOWERS),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        _btnFollowers.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                showFollowers();
            }
        });

        _btnDirectMessage = new ButtonField(getRes().getString(MENU_SEND_DIRECT_MESSAGE),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        _btnDirectMessage.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                sendDirectMessage();
            }
        });

        _btnViewLocation = new ButtonField(getRes().getString(MENU_VIEW_LOCATION),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        _btnViewLocation.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                viewLocation();
            }
        });

        setTitle(_userTitle);

        add(_description);
        add(_boxNotificationsManager);
        add(_btnDirectMessageManager);
        add(btnReply);
        add(btnTweets);
        add(_btnFollowers);
    }

    public void show(UserItemBase user) {
        _userScreenName = user.ScreenName;
        _user = user;
        _userLocation = (user != null) ? new UserLocation(user.Location) : null;
        showInt();
    }

    public void show(String username) {
        _userScreenName = username;
        _user = null;
        showInt();
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
        showUserData();
        showRelationshipData();
        requestUserData();
        requestRelationshipData();
    }

    protected void makeFormMenu(Menu menu) {
        MenuItem mi;

        if (
            (_relationship != null)
            && (_relationship.Source.Following || _relationship.Source.FollowedBy)
            && (!_isUserMyself)
        )
            menu.add(new MenuItem(getRes(), MENU_SEND_DIRECT_MESSAGE, 1000, 10) {
                public void run() { sendDirectMessage(); }
            });
        menu.add(new MenuItem(getRes(), MENU_SEND_PUBLIC_REPLY, 1000, 10) {
            public void run() { sendReply(); }
        });
        menu.add(new MenuItem(getRes(), MENU_RECENT_TWEETS, 1000, 10) {
            public void run() { recentTweets(); }
        });
        if (!_isUserMyself)
            menu.add(new MenuItem(getRes(), MENU_USER_FOLLOWERS, 1000, 10) {
                public void run() { showFollowers(); }
            });
        if ((_userLocation != null) && _userLocation.hasCoordinates())
            menu.add(new MenuItem(getRes(), MENU_VIEW_LOCATION, 1000, 10) {
                public void run() { viewLocation(); }
            });

        if (_relationshipReceived) {
            menu.addSeparator();
            if ((_relationship != null) && _relationship.Source.Following)
                menu.add(new MenuItem(getRes(), MENU_UNFOLLOW, 1000, 10) {
                    public void run() { unfollow(); }
                });
            else
                menu.add(new MenuItem(getRes(), MENU_FOLLOW, 1000, 10) {
                    public void run() { follow(); }
                });
        }

        addBackMenuItem(menu, true);
    }

    //

    private void showInt() {
        _isUserMyself = Options.getInstance().isMyself(_userScreenName);
        if (_isUserMyself)
            try { delete(_btnFollowers); } catch (Exception ignored) {}
        show();
    }

    private void requestUserData() {
        _loadUserRequestID = TwitterManager.getInstance().usersShow(_userScreenName);
    }

    private void requestRelationshipData() {
        if (_isUserMyself)
            return;
        _loadRelationshipRequestID = TwitterManager.getInstance().friendshipsShow(
            Options.getInstance().getUsername(), _userScreenName);
    }

    private void showUserData() {
        /*
        Field title = getTitle();
        if ((title != null) && (title instanceof LabelField))
            ((LabelField)title).setText(_userScreenName);
        else
            setTitle(_userScreenName);
        */

        if (_user == null)
            _userTitle.setUser(_userScreenName);
        else
            _userTitle.setUser(_user);

        StringBuffer desc = new StringBuffer();
        if ((_user != null) && (_user.Description != null) && (_user.Description.length() > 0)) {
            desc.append(_user.Description);
            desc.append('\n');
        }
        if ((_user != null) && (_user.Url != null) && (_user.Url.length() > 0)) {
            desc.append(_user.Url);
            desc.append('\n');
        }
        if ((_userLocation != null) && (_userLocation.getText().length() > 0)) {
            desc.append(getRes().getString(TITLE_LOCATION));
            desc.append(": ");
            desc.append(_user.Location);
            desc.append('\n');
        }
        _description.setText(desc.toString());

        if ((_userLocation != null) && _userLocation.hasCoordinates()) {
            if (indexOfField(_btnViewLocation) < 0) {
                int index = indexOfField(_description);
                insert(_btnViewLocation, index + 1);
            }
        }
        else {
            if (indexOfField(_btnViewLocation) >= 0)
                delete(_btnViewLocation);
        }
    }

    private void showRelationshipData() {
        if (
            (_relationship == null)
            || (!(_relationship.Source.Following || _relationship.Source.FollowedBy))
            || _isUserMyself
        ) {
            _btnDirectMessageManager.deleteAll();
            _boxNotificationsManager.deleteAll();
            return;
        }

        _boxNotifications.setChecked(_relationship.Source.NotificationsEnabled);

        if (_btnDirectMessageManager.getFieldCount() == 0)
            _btnDirectMessageManager.add(_btnDirectMessage);
        if (_boxNotificationsManager.getFieldCount() == 0)
            _boxNotificationsManager.add(_boxNotifications);
    }

    private void follow() {
        _updateFollowRequestID = TwitterManager.getInstance().friendshipsCreate(_userScreenName, false);
        showProgress(getRes().getString(MSG_UPDATING));
    }

    private void unfollow() {
        _updateFollowRequestID = TwitterManager.getInstance().friendshipsDestroy(_userScreenName);
        showProgress(getRes().getString(MSG_UPDATING));
    }

    private void sendReply() {
        new NewMessageScreen().showReply(_userScreenName);
    }

    private void recentTweets() {
        new UserStatusesScreen().show(_userScreenName);
    }

    private void showFollowers() {
        new UserFollowersScreen().show(_userScreenName);
    }

    private void sendDirectMessage() {
        new NewMessageScreen().showDirectMessage(_userScreenName);
    }

    private void viewLocation() {
        if ((_userLocation == null) || (!_userLocation.hasCoordinates()))
            return;
        AppUtils.showLocation(_userLocation.getLatitude(), _userLocation.getLongitude());
    }

    private void updateUser(UserItemBase user) {
        _userScreenName = user.ScreenName;
        _user = user;
        _userLocation = (user != null) ? new UserLocation(user.Location) : null;
        showUserData();
    }

    private void updateRelationship(RelationshipItem data) {
        _relationshipReceived = true;
        _relationship = data;
        showRelationshipData();
    }

    private void sendNotifications() {
        if (_isUserMyself) {
            showUserData();
            return;
        }
        if (_boxNotifications.getChecked())
            _updateNotificationsRequestID = TwitterManager.getInstance().notificationsFollow(_userScreenName);
        else
            _updateNotificationsRequestID = TwitterManager.getInstance().notificationsLeave(_userScreenName);
        showProgress(getRes().getString(MSG_UPDATING));
    }

    // UserTitleManager

    private static class UserTitleManager extends Manager {

        private static final int H_SPACE = 8;
        private static final int V_SPACE = 4;

        private UserIconField _icon = null;
        private LabelField _screenName = null;
        private LabelField _name = null;
        private boolean _initialized = false;
        private boolean _layoutPerformed = false;

        public UserTitleManager() {
            super(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);
        }

        private void setUser(String icon, String screenName, String name) {
            _initialized = false;
            deleteAll();

            _icon = new UserIconField(icon);
            _screenName = new LabelField(screenName);
            _name = new LabelField((name != null) ? name : "");

            add(_icon);
            add(_screenName);
            add(_name);

            _initialized = true;
            if (_layoutPerformed)
                updateLayout();
        }

        public void setUser(UserItemBase user) {
            setUser(user.ProfileImageUrl, user.ScreenName, user.Name);
        }

        public void setUser(String screenName) {
            setUser(null, screenName, null);
        }

        protected void sublayout(int width, int height) {
            if (!_initialized)
                return;
            _layoutPerformed = true;

            layoutChild(_icon, _icon.getPreferredWidth(), _icon.getPreferredHeight());

            int textLeft = _icon.getWidth() + (H_SPACE * 2);
            int textWidth = width - textLeft - H_SPACE;
            layoutChild(_screenName, textWidth, height);
            layoutChild(_name, textWidth, height);

            setPositionChild(_icon, H_SPACE, V_SPACE);
            setPositionChild(_screenName, textLeft, V_SPACE);
            setPositionChild(_name, textLeft, _screenName.getHeight() + (V_SPACE * 2));

            height = Math.max(
                _icon.getHeight() + (V_SPACE * 2),
                _screenName.getHeight() + _name.getHeight() + (V_SPACE * 3)
            );
            setExtent(width, height);
        }
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loadRelationshipRequestID) {
            _loadRelationshipRequestID = -1;
            if ((result != null) && (result instanceof RelationshipItem))
                _app.invokeLater(new RunnableImpl(result) { public void run() {
                    updateRelationship((RelationshipItem)data0);
                }});
        }
        else if (id == _loadUserRequestID) {
            _loadUserRequestID = -1;
            if ((result != null) && (result instanceof UserItemBase))
                _app.invokeLater(new RunnableImpl(result) { public void run() {
                    updateUser((UserItemBase)data0);
                }});
        }
        else if ((id == _updateFollowRequestID) || (id == _updateNotificationsRequestID)) {
            hideProgress();
            if (id == _updateFollowRequestID) {
                _updateFollowRequestID = -1;
                FollowingScreen.getInstance().refreshDataIfVisible();
            }
            else if (id == _updateNotificationsRequestID)
                _updateNotificationsRequestID = -1;
            requestRelationshipData();
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _loadRelationshipRequestID) {
            _loadRelationshipRequestID = -1;
            _app.invokeLater(new Runnable() { public void run() {
                updateRelationship(null);
            }});
        }
        else if (id == _loadUserRequestID)
            _loadUserRequestID = -1;
        else if ((id == _updateFollowRequestID) || (id == _updateNotificationsRequestID)) {
            hideProgress();
            boolean showError = true;
            if (id == _updateFollowRequestID)
                _updateFollowRequestID = -1;
            else if (id == _updateNotificationsRequestID) {
                _updateNotificationsRequestID = -1;
                if (
                    (ex instanceof NetworkException)
                    && (((NetworkException)ex).getResponseCode() == HttpConnection.HTTP_FORBIDDEN)
                ) {
                    if (_relationship != null)
                        _relationship.Source.NotificationsEnabled = true;
                    showError = false;
                }
            }
            _app.invokeLater(new Runnable() { public void run() {
                showRelationshipData();
            }});
            if (showError)
                UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if (id == _loadRelationshipRequestID)
            _loadRelationshipRequestID = -1;
        else if (id == _loadUserRequestID)
            _loadUserRequestID = -1;
        else if ((id == _updateFollowRequestID) || (id == _updateNotificationsRequestID)) {
            if (id == _updateFollowRequestID)
                _updateFollowRequestID = -1;
            else if (id == _updateNotificationsRequestID)
                _updateNotificationsRequestID = -1;
            hideProgress();
            _app.invokeLater(new Runnable() { public void run() {
                showRelationshipData();
            }});
        }
    }
}

