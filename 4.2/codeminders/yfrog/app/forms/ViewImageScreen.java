package codeminders.yfrog.app.forms;

import net.rim.blackberry.api.homescreen.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;
import codeminders.yfrog.lib.items.*;
import codeminders.yfrog.lib.network.*;

public class ViewImageScreen extends ScreenBase
    implements NetworkListener
{

    private static final int SCROLL_STEP_H = 10;
    private static final int SCROLL_STEP_V = 10;

    private static final int LOADED_NONE = 0;
    private static final int LOADED_OPTIMIZED = 1;
    private static final int LOADED_FULL = 2;

    private String _url = null;
    private String _urlOptimized = null;

    private long _loadOptimizedRequestID = -1;
    private long _loadFullRequestID = -1;
    private long _loadInfoRequestID = -1;

    private int _lastWidth = -1;
    private EncodedImage _img = null;
    private EncodedImage _imgPreview = null;

    private boolean _fitToScreen = true;
    private int _left = 0;
    private int _top = 0;
    private String _savedUrl = null;

    private boolean _infoLoaded = false;
    private String _urlFull = null;
    private int _imgLoaded = LOADED_NONE;

    public ViewImageScreen() {
        super();
    }

    public void show(String url, EncodedImage thumbnail) {
        _url = url;
        _urlOptimized = new YFrogConnector(null).YFROGoptimizedimageUrl(url, YFrogConnector.DEV_TYPE_IPHONE);
        _img = thumbnail;
        show();
    }

    protected void onDisplay() {
        super.onDisplay();
        NetworkManager.getInstance().addListener(this);
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
        cancelLoad();
    }

    protected void afterShow(boolean firstShow) {
        loadOptimized();
    }

    protected void makeFormMenu(Menu menu) {
        if (isRequestsActive()) {
            addBackMenuItem(menu, true);
            return;
        }

        if ((_img != null) && (
            (_img.getWidth() > getWidth())
            || (_img.getHeight() > getHeight())
        )) {
            if (_fitToScreen)
                menu.add(new MenuItem(getRes(), MENU_ZOOM_FULL, 1000, 10) {
                    public void run() { zoomFull(); }
                });
            else
                menu.add(new MenuItem(getRes(), MENU_ZOOM_FIT, 1000, 10) {
                    public void run() { zoomFit(); }
                });
        }
        if ((_img != null) && (_savedUrl == null) && (_imgLoaded > LOADED_NONE))
            menu.add(new MenuItem(getRes(), MENU_SAVE, 1000, 10) {
                public void run() { saveImage(); }
            });

        if (_imgLoaded < LOADED_FULL)
            menu.add(new MenuItem(getRes(), MENU_LOAD_FULL_IMAGE, 1000, 10) {
                public void run() {
                    if (_urlFull != null)
                        loadFullImage();
                    else
                        loadInfo();
                }
            });

        /* VERSION_DEPENDED: 4.7 * /
        if ((_img != null) && (_imgLoaded > LOADED_NONE)) {
            if (menu.getSize() > 0)
                menu.addSeparator();
            menu.add(new MenuItem(getRes(), MENU_SET_WALLPAPER, 1000, 10) {
                public void run() { setAsWallpaper(); }
            });
            menu.add(new MenuItem(getRes(), MENU_RESET_WALLPAPER, 1000, 10) {
                public void run() { resetWallpaper(); }
            });
        }
        /* */

        addBackMenuItem(menu, true);
    }

    protected void paint(Graphics graphics) {
        super.paint(graphics);
        if (_img == null)
            return;

        int width = getWidth();
        if (_lastWidth != width) {
            createPreview();
            _lastWidth = width;
        }
        int height = getHeight();
        int imgWidth = _imgPreview.getScaledWidth();
        int imgHeight = _imgPreview.getScaledHeight();

        int x = (_left >= 0) ? _left : 0;
        int y = (_top >= 0) ? _top : 0;
        int w = (_left >= 0) ? imgWidth : width;
        int h = (_top >= 0) ? imgHeight : height;
        int imgLeft = (_left >= 0) ? 0 : (-_left);
        int imgTop = (_top >= 0) ? 0 : (-_top);

        graphics.drawImage(x, y, w, h, _imgPreview, 0, imgLeft, imgTop);
    }

    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        if ((_img == null) || (_imgPreview == null) || _fitToScreen)
            return true;
        int oldLeft = _left;
        int oldTop = _top;
        _left -= (dx * SCROLL_STEP_H);
        _top -= (dy * SCROLL_STEP_V);
        normalizePosition();
        if ((oldLeft != _left) || (oldTop != _top))
            invalidate();
        return true;
    }

    /* VERSION_DEPENDED: 4.7 * /

    private boolean _scrollEnabled = false;

    private void touchEventScroll(TouchEvent message) {
        if ((_img == null) || (_imgPreview == null) || _fitToScreen)
            return;
        int size = message.getMovePointsSize();
        if (size < 2)
            return;
        int[] x = new int[size];
        int[] y = new int[size];
        message.getMovePoints(1, x, y, null);
        int offsX = x[size - 1] - x[size - 2];
        int offsY = y[size - 1] - y[size - 2];
        _left += (offsX * 2);
        _top += (offsY * 2);
        normalizePosition();
        invalidate();
    }

    protected boolean touchEvent(TouchEvent message) {
        if (!message.isValid())
            return false;
        try {
            switch (message.getEvent()) {
                case TouchEvent.DOWN:
                    if ((_img != null) && (_imgPreview != null) && (!_fitToScreen))
                        _scrollEnabled = true;
                    return true;
                case TouchEvent.UP:
                    _scrollEnabled = false;
                    return true;
                case TouchEvent.CANCEL:
                    _scrollEnabled = false;
                    return true;
                case TouchEvent.MOVE:
                    if (_scrollEnabled)
                        touchEventScroll(message);
                    return true;
            }
        }
        catch (Exception ignored) { }
        return true;
    }

    /* */
    //

    private void normalizePosition() {
        int thisW = getWidth();
        int thisH = getHeight();
        int imgW = _imgPreview.getScaledWidth();
        int imgH = _imgPreview.getScaledHeight();

        if (imgW <= thisW)
            _left = (thisW - imgW) / 2;
        else {
            if ((_left + imgW) < thisW)
                _left = thisW - imgW;
            if (_left > 0)
                _left = 0;
        }

        if (imgH <= thisH)
            _top = (thisH - imgH) / 2;
        else {
            if ((_top + imgH) < thisH)
                _top = thisH - imgH;
            if (_top > 0)
                _top = 0;
        }
    }

    private void fullInvalidate() {
        _lastWidth = -1;
        invalidate();
    }

    private boolean isRequestsActive() {
        return (_loadInfoRequestID >= 0)
            || (_loadOptimizedRequestID >= 0)
            || (_loadFullRequestID >= 0);
    }

    private void showLoadingProgress() {
        if (!isRequestsActive())
            return;
        showProgress(getRes().getString(MSG_LOADING), new Runnable() {
            public void run() { cancelLoad(); }
        });
    }

    private void tryHideProgress() {
        if (isRequestsActive())
            return;
        hideProgress();
    }

    private void loadOptimized() {
        _loadOptimizedRequestID = NetworkManager.getInstance().startImageRequest(_urlOptimized);
        showLoadingProgress();
    }

    private void loadInfo() {
        _loadInfoRequestID = new YFrogConnector(Options.getInstance().getYFrogAppKey()).YFROGxmlInfo(_url);
        showLoadingProgress();
    }

    private void loadFullImage() {
        if (_urlFull != null)
            _loadFullRequestID = NetworkManager.getInstance().startImageRequest(_urlFull);
        showLoadingProgress();
    }

    private void cancelLoad() {
        if (_loadInfoRequestID >= 0) {
            NetworkManager.getInstance().cancelRequest(_loadInfoRequestID);
            _loadInfoRequestID = -1;
        }
        if (_loadOptimizedRequestID >= 0) {
            NetworkManager.getInstance().cancelRequest(_loadOptimizedRequestID);
            _loadOptimizedRequestID = -1;
        }
        if (_loadFullRequestID >= 0) {
            NetworkManager.getInstance().cancelRequest(_loadFullRequestID);
            _loadFullRequestID = -1;
        }
        hideProgress();
    }

    private void createPreview() {
        if (
            (_img == null)
            || (!_fitToScreen)
            || ((_img.getWidth() <= getWidth()) && (_img.getHeight() <= getHeight()))
        )
            _imgPreview = _img;
        else
            _imgPreview = UiUtils.zoomImage(_img, getWidth(), getHeight());

        if (_imgPreview == null) {
            _left = 0;
            _top = 0;
        }
        else {
            _left = (getWidth() - _imgPreview.getScaledWidth()) / 2;
            _top = (getHeight() - _imgPreview.getScaledHeight()) / 2;
        }
    }

    private void zoomFull() {
        _fitToScreen = false;
        fullInvalidate();
    }

    private void zoomFit() {
        _fitToScreen = true;
        fullInvalidate();
    }

    private void saveImage() {
        if ((_img == null) || (_savedUrl != null))
            return;
        new SaveThread(this, SaveThread.SAVE_ONLY).start();
    }

    /* VERSION_DEPENDED: 4.7 * /

    private void setAsWallpaper() {
        if (_img == null)
            return;
        new SaveThread(this, SaveThread.SET_WALLPAPER).start();
    }

    private void resetWallpaper() {
        new SaveThread(this, SaveThread.RESET_WALLPAPER).start();
    }

    /* */

    private void setImage(EncodedImage img, int loadedMode) {
        _app.invokeLater(new RunnableImpl(img, loadedMode) { public void run() {
            _img = (EncodedImage)data0;
            _imgLoaded = int0;
            _savedUrl = null;
            fullInvalidate();
        }});
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

        if (_urlFull != null) {
            loadFullImage();
        }
        else {
            UiUtils.alert(getRes().getString(MSG_NO_IMAGE_INFO));
        }
    }

    // SaveThread

    private static class SaveThread extends Thread {

        public static int SAVE_ONLY = 0;
        public static int SET_WALLPAPER = 1;
        public static int RESET_WALLPAPER = 2;

        private ViewImageScreen _owner;
        private int _mode;

        public SaveThread(ViewImageScreen owner, int mode) {
            _owner = owner;
            _mode = mode;
        }

        public void run() {
            _owner.showProgress(ResManager.getString(MSG_SAVING));
            try {
                if (
                    ((_mode == SAVE_ONLY) || (_mode == SET_WALLPAPER))
                    && StringUtils.isNullOrEmpty(_owner._savedUrl)
                )
                    _owner._savedUrl = AppUtils.saveYFrogImage(_owner._url, _owner._img);
                if (_mode == SAVE_ONLY)
                    UiUtils.alert(ResManager.getString(MSG_IMAGE_SAVED));
                /* VERSION_DEPENDED: 4.7 * /
                if ((_mode == SET_WALLPAPER) && (!StringUtils.isNullOrEmpty(_owner._savedUrl)))
                    HomeScreen.setBackgroundImage(_owner._savedUrl);
                if (_mode == RESET_WALLPAPER)
                    HomeScreen.setBackgroundImage(null);
                /* */
            }
            catch (Exception ex) {
                UiUtils.alert(ex);
            }
            finally {
                _owner.hideProgress();
            }
        }
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loadOptimizedRequestID) {
            _loadOptimizedRequestID = -1;
            if ((result != null) && (result instanceof EncodedImage))
                setImage((EncodedImage)result, LOADED_OPTIMIZED);
        }
        else if (id == _loadFullRequestID) {
            _loadFullRequestID = -1;
            if ((result != null) && (result instanceof EncodedImage))
                setImage((EncodedImage)result, LOADED_FULL);
        }
        else if (id == _loadInfoRequestID) {
            _loadInfoRequestID = -1;
            if ((result != null) && (result instanceof ImgInfo))
                setInfo((ImgInfo)result);
        }
        else
            return;
        tryHideProgress();
    }

    public void error(long id, Throwable ex) {
        if (id == _loadInfoRequestID)
            _loadInfoRequestID = -1;
        else if (id == _loadOptimizedRequestID)
            _loadOptimizedRequestID = -1;
        else if (id == _loadFullRequestID)
            _loadFullRequestID = -1;
        else
            return;
        tryHideProgress();
        UiUtils.alert(ex);
    }

    public void cancelled(long id) {
        if (id == _loadInfoRequestID)
            _loadInfoRequestID = -1;
        else if (id == _loadOptimizedRequestID)
            _loadOptimizedRequestID = -1;
        else if (id == _loadFullRequestID)
            _loadFullRequestID = -1;
        else
            return;
        tryHideProgress();
    }
}

