package codeminders.yfrog.app.forms;

import javax.microedition.media.*;
import javax.microedition.media.control.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;

public class ViewVideoScreen extends ScreenBase
    implements NetworkListener, PlayerListener
{

    private String _url = null;
    private long _loadInfoRequestID = -1;
    private long _loadFileRequestID = -1;

    private boolean _infoLoaded = false;
    private String _urlFull = null;

    private EncodedImage _img = null;
    private Player _player = null;
    private VideoControl _videoControl = null;
    private StartVideoThread _startThread = null;
    private String _savedUrlTmp = null;
    private String _savedUrl = null;

    public ViewVideoScreen() {
        super();
    }

    public void show(String url, EncodedImage thumbnail) {
        _url = url;
        _img = thumbnail;
        show();
    }

    public boolean onClose() {
        cancelLoad(false);
        closePlayer();
        return true;
    }

    protected void onDisplay() {
        super.onDisplay();
        NetworkManager.getInstance().addListener(this);
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
    }

    protected void afterShow(boolean firstShow) {
        loadInfo();
    }

    protected void paint(Graphics graphics) {
        super.paint(graphics);
        if ((getFieldCount() > 0) || (_img == null))
            return;
        int width = getWidth();
        int height = getHeight();
        int imgWidth = _img.getScaledWidth();
        int imgHeight = _img.getScaledHeight();
        int x = (imgWidth < width) ? ((width - imgWidth) / 2) : 0;
        int y = (imgHeight < height) ? ((height - imgHeight) / 2) : 0;
        int w = (imgWidth < width) ? imgWidth : width;
        int h = (imgHeight < height) ? imgHeight : height;
        graphics.drawImage(x, y, w, h, _img, 0, 0, 0);
    }

    protected void makeFormMenu(Menu menu) {
        if ((_loadInfoRequestID >= 0) || (_startThread != null)) {
            addBackMenuItem(menu, true);
            return;
        }

        if ((_player == null) || (_player.getState() != Player.STARTED))
            menu.add(new MenuItem(getRes(), MENU_PLAY, 1000, 10) {
                public void run() {
                    if (_player == null)
                        startPlayer();
                    else
                        try { _player.start(); } catch (Exception ignored) {}
                }
            });
        if ((_player != null) && (_player.getState() == Player.STARTED))
            menu.add(new MenuItem(getRes(), MENU_STOP, 1000, 10) {
                public void run() {
                    if (_player != null)
                        try { _player.stop(); } catch (Exception ignored) {}
                }
            });

        if ((_urlFull != null) && (_savedUrl == null))
            menu.add(new MenuItem(getRes(), MENU_SAVE, 1000, 10) {
                public void run() { saveVideo(); }
            });

        addBackMenuItem(menu, true);
    }

    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case Characters.SPACE:
                if (_player != null) {
                    if (_player.getState() == Player.STARTED)
                        try { _player.stop(); } catch (Exception ignored) {}
                    else
                        try { _player.start(); } catch (Exception ignored) {}
                    return true;
                }
                break;
        }
        return super.keyChar(c, status, time);
    }

    //

    private static String[] NO_H264 = new String[] {
        "8100", "8110", "8120", "8130",
        "8220",
        "8300", "8310", "8320", "8330", "8350",
        "8800", "8820", "8830"
    };

    public static boolean isPlaybackSupported(String url) {
        if (YFrogConnector.YFROGgetMediaType(url) != YFrogConnector.MEDIATYPE_MP4)
            return false;
        if (DeviceInfo.isSimulator())
            return true;
        if (SysUtils.getOSVersionNum() < SysUtils.OS430)
            return false;
        String modelNum = DeviceInfo.getDeviceName();
        if (StringUtils.isNullOrEmpty(modelNum))
            return false;
        for (int i = NO_H264.length - 1; i >= 0; i--)
            if (modelNum.startsWith(NO_H264[i]))
                return false;
        return true;
    }

    private String getMediaErrorInfo(Object error) {
        int code = 0;
        if (error != null)
            try { code = Integer.parseInt(error.toString()); } catch (Exception ignored) {}
        String[] messages = getRes().getStringArray(MSG_VIDEO_ERROR);
        if (code >= messages.length)
            code = messages.length;
        return messages[code];
    }

    private void loadInfo() {
        _loadInfoRequestID = new YFrogConnector(Options.getInstance().getYFrogAppKey())
            .YFROGxmlInfo(_url);
        showProgress(getRes().getString(MSG_LOADING), new Runnable() {
            public void run() { cancelLoad(true); }
        });
    }

    private void cancelLoad(boolean noDataClose) {
        if (_loadInfoRequestID >= 0) {
            NetworkManager.getInstance().cancelRequest(_loadInfoRequestID);
            _loadInfoRequestID = -1;
        }
        if (_loadFileRequestID >= 0) {
            NetworkManager.getInstance().cancelRequest(_loadFileRequestID);
        }
        hideProgress();
        if (noDataClose && (!_infoLoaded))
            close();
    }

    private void setInfo(ImgInfo info) {
        _infoLoaded = true;
        _urlFull = null;

        if (
            (info != null)
            && (info.links != null)
            && (!StringUtils.isNullOrEmpty(info.links.image_link))
        )
            _urlFull = info.links.image_link;

        if (_urlFull != null)
            startPlayer();
        else {
            UiUtils.alert(getRes().getString(MSG_NO_VIDEO_INFO));
            close();
        }
    }

    private void closePlayer() {
        closePlayer(_player, _videoControl);
        _player = null;
        _videoControl = null;
    }

    private void closePlayer(Player player, VideoControl videoControl) {
        if (Application.isEventDispatchThread())
            try { deleteAll(); } catch (Exception ignored) {}
        else
            synchronized (Application.getEventLock()) { try { deleteAll(); } catch (Exception ignored) {} }
        if (player != null) try { player.stop(); } catch (Exception ignored) {}
        if (videoControl != null) try { videoControl.setVisible(false); } catch (Exception ignored) {}
        if (player != null) try { player.close(); } catch (Exception ignored) {}
        if (player != null) try { player.removePlayerListener(this); } catch (Exception ignored) {}
    }

    private void startPlayer() {
        /* * /
        net.rim.blackberry.api.browser.Browser.getDefaultSession().displayPage(_urlFull);
        close();
        /* */

        /* */
        if (_player != null)
            return;
        _startThread = new StartVideoThread(this);
        showProgress(ResManager.getString(MSG_LOADING),
            new RunnableImpl(_startThread) { public void run() {
                ((StartVideoThread)data0).cancel();
                hideProgress();
                close();
            }}
        );
        _startThread.start();
        /* */
    }

    private void saveVideo() {
        if ((_urlFull == null) || (_savedUrl != null))
            return;
        try {
            _savedUrlTmp = AppUtils.prepareSaveYFrogVideo(_url, _urlFull);
            _loadFileRequestID = NetworkManager.getInstance().startFileRequest(
                _urlFull, _savedUrlTmp);
            showProgress(ResManager.getString(MSG_SAVING),
                new Runnable() { public void run() { cancelLoad(false); }}
            );
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
        }
    }

    // StartVideoThread

    private static class StartVideoThread extends Thread {

        private ViewVideoScreen _owner;
        private boolean _cancelled = false;
        private Player _player = null;
        private VideoControl _videoControl = null;
        private StartVideoThread _this;

        public StartVideoThread(ViewVideoScreen owner) {
            _owner = owner;
            _this = this;
        }

        public void cancel() {
            _cancelled = true;
            interrupt();
        }

        public void run() {
            try {
                String requestUrl = _owner._urlFull
                    + NetworkManager.getInstance().getConnectionOptions();
                //System.out.println("Media player connecting: " + requestUrl);
                _player = javax.microedition.media.Manager.createPlayer(requestUrl);
                _player.addPlayerListener(_owner);
                _player.realize();
                _videoControl = (VideoControl)_player.getControl("VideoControl");
                Field f = (Field)_videoControl.initDisplayMode(
                    VideoControl.USE_GUI_PRIMITIVE, "net.rim.device.api.ui.Field");
                synchronized (Application.getEventLock()) { _owner.add(f); }
                _videoControl.setDisplayFullScreen(true);
                _videoControl.setVisible(true);
                _player.prefetch();
                _player.start();
                _owner._player = _player;
                _owner._videoControl = _videoControl;
                _owner.hideProgress();
            }
            catch (Exception ex) {
                boolean displayed;
                synchronized (Application.getEventLock()) { displayed = _owner.isDisplayed(); }
                if (displayed) {
                    try { _owner.hideProgress(); } catch (Exception ignored) {}
                    if (!_cancelled)
                        UiUtils.alert(ex);
                }
                try { _owner.closePlayer(_player, _videoControl); } catch (Exception ignored) {}
                if (displayed)
                    _owner.close();
            }
            finally {
                _owner._startThread = null;
            }
        }
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loadInfoRequestID) {
            _loadInfoRequestID = -1;
            hideProgress();
            if ((result != null) && (result instanceof ImgInfo))
                setInfo((ImgInfo)result);
        }
        else if (id == _loadFileRequestID) {
            _loadFileRequestID = -1;
            hideProgress();
            if ((result != null) && (result instanceof String))
                _savedUrl = (String)result;
            UiUtils.alert(getRes().getString(MSG_VIDEO_SAVED));
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _loadInfoRequestID)
            _loadInfoRequestID = -1;
        else if (id == _loadFileRequestID)
            _loadFileRequestID = -1;
        else
            return;
        hideProgress();
        UiUtils.alert(ex);
        if (!_infoLoaded)
            close();
    }

    public void cancelled(long id) {
        if (id == _loadInfoRequestID)
            _loadInfoRequestID = -1;
        else if (id == _loadFileRequestID) {
            _loadFileRequestID = -1;
            try { FileUtils.deleteFile(_savedUrlTmp); } catch (Exception ignored) {}
        }
        else
            return;
        hideProgress();
        if (!_infoLoaded)
            close();
    }

    // PlayerListener

    public void playerUpdate(Player player, String event, Object eventData) {
        //System.out.println("YFrog Media Player: " + event + ((eventData != null) ? (" " + eventData) : ""));
        if (PlayerListener.END_OF_MEDIA.equals(event))
            closePlayer();
        else if (PlayerListener.ERROR.equals(event)) {
            closePlayer();
            boolean displayed;
            if (Application.isEventDispatchThread())
                displayed = isDisplayed();
            else
                synchronized (Application.getEventLock()) { displayed = isDisplayed(); }
            if (displayed)
                UiUtils.alert(getMediaErrorInfo(eventData));
        }
    }
}

