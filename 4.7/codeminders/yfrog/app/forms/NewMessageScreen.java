package codeminders.yfrog.app.forms;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.location.*;
import net.rim.blackberry.api.invoke.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.data.items.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.app.controls.*;

public class NewMessageScreen extends ScreenBase
    implements NetworkListener, FileBrowserCallback, LocationDataListener
{

    private EditField _text;
    private StatusField _status;
    private CheckboxField _boxMyLocation;
    //private CheckboxField _boxMediaLocation;
    private CheckboxField _boxTextLocation;

    private long _sendRequestID = -1;
    private long _replyID = -1;
    private String _directMessageScreenName = null;

    private String _fileUrl = null;
    private Location _location = null;
    private String _textLocationUrl = null;

    public NewMessageScreen() {
        super(TITLE_NEW_TWEET);

        _boxMyLocation = new CheckboxField(getRes().getString(LABEL_UPDATE_MY_LOCATION), false);
        //_boxMediaLocation = new CheckboxField(getRes().getString(LABEL_UPDATE_MEDIA_LOCATION), false);
        _boxTextLocation = new CheckboxField(getRes().getString(LABEL_UPDATE_TEXT_LOCATION), true);
        _boxTextLocation.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                _app.invokeLater(new Runnable() {
                    public void run() { updateTextLocation(); }
                });
            }
        });

        _text = new ActiveAutoTextEditField("", "") {
            protected void displayFieldFullMessage() { }
        };
        _text.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                updateStatus();
            }
        });

        _status = new StatusField(this);

        add(_text);
        setStatus(_status);
        setAutoFocusField(_text);
    }

    public void showReply(DirectMessageItem message) {
        setTitle(getRes().getString(TITLE_NEW_DIRECT_MESSAGE));
        _directMessageScreenName = message.SenderScreenName;
        show();
    }

    public void showReply(StatusItem status) {
        _replyID = status.ID;
        showReply(status.User.ScreenName);
    }

    public void showReply(SearchResultItem searchResult) {
        showReply(searchResult.FromUser);
    }

    public void showReply(String username) {
        StringBuffer text = new StringBuffer();
        text.append('@');
        text.append(username);
        text.append(' ');
        _text.setText(text.toString());

        show();
    }

    public void showRetweet(StatusItem status) {
        showRetweet(status.User.ScreenName, status.Text);
    }

    public void showRetweet(DirectMessageItem message) {
        showRetweet(message.SenderScreenName, message.Text);
    }

    public void showRetweet(SearchResultItem searchResult) {
        showRetweet(searchResult.FromUser, searchResult.Text);
    }

    private void showRetweet(String screenName, String text) {
        StringBuffer msgText = new StringBuffer();
        msgText.append("RT @");
        msgText.append(screenName);
        msgText.append(": ");
        msgText.append(text);
        _text.setText(msgText.toString());

        show();
    }

    public void showDirectMessage(String screenName) {
        setTitle(getRes().getString(TITLE_NEW_DIRECT_MESSAGE));
        _directMessageScreenName = screenName;
        show();
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        DataManager.getInstance().addSendListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
        DataManager.getInstance().removeSendListener(this);
        DataManager.getInstance().removeLocationListener(this);
    }

    protected void afterShow(boolean firstShow) {
        UiUtils.setCursorPosition(_text, _text.getText().length());
        updateStatus();
        setDirty(false);
    }

    protected void makeFormMenu(Menu menu) {
        MenuItem def = null;
        MenuItem mi;

        if (_text.getText().length() > 0) {
            mi = new MenuItem(getRes(), MENU_SEND, 1000, 10) {
                public void run() { send(); }
            };
            menu.add(mi);
            def = mi;

            menu.add(new MenuItem(getRes(), MENU_QUEUE, 1000, 10) {
                public void run() { queueAndClose(); }
            });
        }

        if (menu.getSize() > 0)
            menu.addSeparator();
        if (DataManager.getInstance().isLocationEnabled())
            menu.add(new MenuItem(getRes(), MENU_GET_LOCATION, 1000, 10) {
                public void run() { getLocation(); }
            });
        menu.add(new MenuItem(getRes(), MENU_ATTACH_MEDIA, 1000, 10) {
            public void run() { attachMedia(); }
        });

        menu.addSeparator();
        menu.add(new MenuItem(getRes(), MENU_CANCEL, 1000, 10) {
            public void run() { close(); }
        });

        if (def != null)
            menu.setDefault(def);
    }

    public boolean onClose() {
        if (!isDirty())
            return true;
        int res = UiUtils.ask(
            getRes().getString(MSG_TWEET_NOT_SENT),
            new String[] {
                getRes().getString(MENU_OK),
                getRes().getString(MENU_CANCEL),
                getRes().getString(MENU_QUEUE)
            },
            new int[] {
                Dialog.OK,
                Dialog.CANCEL,
                Dialog.SAVE
            },
            Dialog.OK
        );
        switch (res) {
            case Dialog.OK:
                setDirty(false);
                return true;
            case Dialog.SAVE:
                if (!queue())
                    break;
                return true;
        }
        return false;
    }

    //

    private int getLocationOptions() {
        if (_location == null)
            return 0;
        int res = 0;
        if (_boxTextLocation.getChecked())
            res |= UnsentItemBase.LOCATION_TEXT;
        //if ((_fileUrl != null) && _boxMediaLocation.getChecked())
            res |= UnsentItemBase.LOCATION_MEDIA;
        if (_boxMyLocation.getChecked())
            res |= UnsentItemBase.LOCATION_PROFILE;
        return res;
    }

    private Double getLatitude() {
        return (_location != null) ? new Double(_location.getQualifiedCoordinates().getLatitude()) : null;
    }

    private Double getLongitude() {
        return (_location != null) ? new Double(_location.getQualifiedCoordinates().getLongitude()) : null;
    }

    private void updateStatus() {
        _status.setCharsCount(
            TwitterManager.MAX_STATUS_TEXT_SIZE - _text.getText().length());
    }

    private void updateTextLocation() {
        if ((_location != null) && _boxTextLocation.getChecked()) {
            if (_textLocationUrl == null)
                _textLocationUrl = DataManager.generateTinyUrl();
            String text = _text.getText();
            if (text.indexOf(_textLocationUrl) < 0) {
                if (text.length() > 0)
                    text += " ";
                text += _textLocationUrl;
                text += " ";
                _text.setText(text);
                UiUtils.setCursorPosition(_text, text.length());
            }
        }
        else {
            if (_textLocationUrl != null) {
                String text = _text.getText();
                int cursorPos = _text.getCursorPosition();
                int pos = text.indexOf(_textLocationUrl);
                if (pos >= 0) {
                    text = text.substring(0, pos) + text.substring(pos + _textLocationUrl.length());
                    if (cursorPos > text.length())
                        cursorPos = text.length();
                    _text.setText(text);
                    UiUtils.setCursorPosition(_text, cursorPos);
                }
                _textLocationUrl = null;
            }
        }
    }

    private UnsentItemBase createItemToSend() {
        if (_directMessageScreenName != null)
            return new UnsentDirectMessageItem(
                Options.getInstance().getUsername(),
                _directMessageScreenName,
                _text.getText(),
                _fileUrl,
                getLocationOptions(), getLatitude(), getLongitude(), _textLocationUrl
            );
        else
            return new UnsentStatusItem(
                Options.getInstance().getUsername(),
                _text.getText(),
                _replyID,
                _fileUrl,
                getLocationOptions(), getLatitude(), getLongitude(), _textLocationUrl
            );
    }

    private boolean checkBeforeSend() {
        if (_text.getText().length() > TwitterManager.MAX_STATUS_TEXT_SIZE) {
            UiUtils.alert(getRes().getString(MSG_MESSAGE_TOO_LONG));
            return false;
        }
        return true;
    }

    private boolean queue() {
        if (!checkBeforeSend())
            return false;
        UnsentItemBase item = createItemToSend();
        DataManager.getInstance().addUnsentItem(item);
        setDirty(false);
        return true;
    }

    private void queueAndClose() {
        if (!queue())
            return;
        close();
    }

    private void send() {
        if (!checkBeforeSend())
            return;
        if ((_sendRequestID > 0) || (_text.getText().length() == 0))
            return;
        UnsentItemBase item = createItemToSend();
        try {
            _sendRequestID = DataManager.getInstance().send(item);
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
            return;
        }

        if (_directMessageScreenName != null)
            showProgress(getRes().getString(MSG_SENDING_NEW_DIRECT_MESSAGE));
        else
            showProgress(getRes().getString(MSG_SENDING_NEW_TWEET));
    }

    private void attachMedia() {
        try {
            Menu menu = new Menu();
            menu.add(new MenuItem(getRes(), MENU_BROWSE_MEDIA, 1000, 10) {
                public void run() { attachFile(); }
            });
            if (DeviceInfo.hasCamera()) {
                menu.add(new MenuItem(getRes(), MENU_CAMERA, 1000, 10) {
                    public void run() { attachCamera(); }
                });
                /* VERSION_DEPENDED: 4.7 */
                menu.add(new MenuItem(getRes(), MENU_VIDEO, 1000, 10) {
                    public void run() { attachVideo(); }
                });
                /* */
            }
            menu.show();
        }
        catch (Exception ex) {
            UiUtils.alertMenuError(ex);
        }
    }

    private void attachFile() {
        new FileBrowserScreen().show(this);
    }

    /* VERSION_DEPENDED_NOT: 4.7 * /
    private void attachCamera() {
        Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA,
            new CameraArguments());
        new FileBrowserScreen().showMediaMonitor(this);
    }
    /* */

    /* VERSION_DEPENDED: 4.7 */
    private void attachCamera() {
        Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA,
            new CameraArguments(CameraArguments.ARG_CAMERA_APP));
        new FileBrowserScreen().showMediaMonitor(this);
    }

    private void attachVideo() {
        Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA,
            new CameraArguments(CameraArguments.ARG_VIDEO_RECORDER));
        new FileBrowserScreen().showMediaMonitor(this);
    }
    /* */

    private void getLocation() {
        showProgress(getRes().getString(MSG_RETRIEVING_LOCATION), new Runnable() {
            public void run() {
                DataManager.getInstance().cancelReceiveLocation();
                hideProgress();
            }
        });
        DataManager.getInstance().addLocationListener(this);
        DataManager.getInstance().startReceiveLocation();
    }

    private void onLocationReceived(Location location) {
        _location = location;
        _status.setLocation(_location);
        updateTextLocation();
    }

    // StatusField

    private static class StatusField extends Manager implements ImageDataListener, DrawStyle {

        private static final int IMAGE_SIZE = 64;

        private NewMessageScreen _owner;

        private String _imageUrl = null;
        private Bitmap _bmp = null;
        private int _charsCount = 0;
        private int _charsCountWidth = 0;
        private boolean _layoutPerformed = false;
        private String[] _location = null;

        public StatusField(NewMessageScreen owner) {
            super(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);
            _owner = owner;
        }

        protected void onDisplay() {
            super.onDisplay();
            DataManager.getInstance().addImageFileListener(this);
        }

        protected void onUndisplay() {
            super.onUndisplay();
            DataManager.getInstance().removeImageFileListener(this);
        }

        protected void sublayout(int width, int height) {
            _layoutPerformed = true;
            Font font = Font.getDefault();
            int fontHeight = font.getHeight();

            int totalHeight = fontHeight;
            _charsCountWidth = font.getAdvance("888") + 4;
            int restWidth = width - _charsCountWidth - 8;

            if (_bmp != null) {
                restWidth -= (_bmp.getWidth() + 8);
                totalHeight = Math.max(totalHeight, _bmp.getHeight());
            }

            if (_location != null) {
                int locationHeight = _location.length * fontHeight;
                int fieldCount = getFieldCount();
                for (int i = 0; i < fieldCount; i++) {
                    Field f = getField(i);
                    layoutChild(f, restWidth, height);
                    setPositionChild(f, 0, locationHeight);
                    locationHeight += f.getHeight();
                }
                totalHeight = Math.max(totalHeight, locationHeight);
            }

            setExtent(width, totalHeight);
        }

        protected void paint(Graphics graphics) {
            super.paint(graphics);
            Font font = Font.getDefault();
            graphics.setFont(font);

            int width = getWidth();
            int height = getHeight();
            int fontHeight = font.getHeight();
            // chars count
            String charsText = Integer.toString(_charsCount);
            width -= _charsCountWidth;
            graphics.drawText(charsText, width, height - fontHeight);
            // bitmap
            if (_bmp != null) {
                int x = 0;
                if (_location != null) {
                    width -= (_bmp.getWidth() + 8);
                    x = width;
                }
                graphics.drawBitmap(x, height - _bmp.getHeight(),
                    _bmp.getWidth(), _bmp.getHeight(), _bmp, 0, 0);
            }
            // location
            if (_location != null) {
                width -= 8;
                int y = 0;
                for (int i = 0; i < _location.length; i++, y += fontHeight)
                    graphics.drawText(_location[i], 0, y, LEFT | TOP | ELLIPSIS, width);
            }
        }

        public void setCharsCount(int value) {
            _charsCount = value;
            invalidate();
        }

        public void setImageFileUrl(String value) {
            _imageUrl = value;
            if (FileUtils.isVideoFile(_imageUrl))
                _bmp = FileBrowserScreen.getVideoBitmap();
            else
                _bmp = FileBrowserScreen.getImageBitmap();
            adjustControls();
            DataManager.getInstance().loadImageFile(_imageUrl);
        }

        public void setLocation(Location location) {
            Coordinates coord = location.getQualifiedCoordinates();
            _location = new String[] {
                "Lon: " + StringUtils.formatDouble(coord.getLongitude(), 5, 0),
                "Lat: " + StringUtils.formatDouble(coord.getLatitude(), 5, 0)
            };
            adjustControls();
        }

        private void adjustControls() {
            deleteAll();
            if (_location != null) {
                add(_owner._boxTextLocation);
                /*
                if (_imageUrl != null)
                    add(_owner._boxMediaLocation);
                */
                add(_owner._boxMyLocation);
            }
        }

        // ImageDataListener

        public void imageLoaded(long requestID, String url, EncodedImage image) {
            if (!StringUtilities.strEqual(_imageUrl, url))
                return;
            EncodedImage img = UiUtils.zoomImage(image, IMAGE_SIZE, IMAGE_SIZE);
            if (img == null)
                return;
            _bmp =img.getBitmap();
            if (_layoutPerformed)
                updateLayout();
        }
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _sendRequestID) {
            _sendRequestID = -1;
            hideProgress();
            _app.invokeLater(new Runnable() {
                public void run() {
                    setDirty(false);
                    close();
                }
            });
            if (_directMessageScreenName != null)
                DirectMessagesScreen.getInstance().refreshDataIfVisible();
            else {
                HomeListScreen.getInstance().refreshDataIfVisible();
                MentionsScreen.getInstance().refreshDataIfVisible();
            }
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _sendRequestID) {
            _sendRequestID = -1;
            hideProgress();
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if (id == _sendRequestID) {
            _sendRequestID = -1;
            hideProgress();
        }
    }

    // FileBrowserCallback

    public void fileSelected(String url) {
        // insert stub string
        String text = _text.getText();
        if (text.indexOf(DataManager.YFROG_STUB_URL) < 0) {
            StringBuffer buf = new StringBuffer();
            int pos = _text.getCursorPosition();
            if (pos > 0) {
                buf.append(text.substring(0, pos));
                buf.append(' ');
            }
            buf.append(DataManager.YFROG_STUB_URL);
            buf.append(' ');
            int newPos = buf.length();
            if (pos < text.length())
                buf.append(text.substring(pos));
            _text.setText(buf.toString());
            UiUtils.setCursorPosition(_text, newPos);
            _text.setDirty(true);
        }
        _fileUrl = url;
        _status.setImageFileUrl(url);
    }

    // LocationDataListener

    public void locationReceived(Location location) {
        hideProgress();
        DataManager.getInstance().removeLocationListener(this);
        _app.invokeLater(new RunnableImpl(location) { public void run() {
            onLocationReceived((Location)data0);
        }});
    }

    public void locationError(Throwable ex) {
        hideProgress();
        DataManager.getInstance().removeLocationListener(this);
        UiUtils.alert(getRes().getString(MSG_NO_LOCATION));
    }

    public void locationCancelled() {
        hideProgress();
        DataManager.getInstance().removeLocationListener(this);
    }
}

