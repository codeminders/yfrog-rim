package codeminders.yfrog.app.controls;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.data.DataManager;
import codeminders.yfrog.data.ImageDataListener;
import codeminders.yfrog.utils.StringUtils;

public class UserIconField extends Field implements ImageDataListener {

    private static final int ICON_WIDTH = 48;
    private static final int ICON_HEIGHT = 48;

    private String _url;
    private EncodedImage _img = null;

    public UserIconField(String url) {
        super(0);
        _url = url;
    }

    protected void onDisplay() {
        super.onDisplay();
        if (StringUtils.isNullOrEmpty(_url))
            return;
        DataManager.getInstance().addIconListener(this);
        _img = DataManager.getInstance().loadIcon(_url);
        if (_img != null) {
            DataManager.getInstance().removeIconListener(this);
            invokeInvalidate();
        }
    }

    protected void onUndisplay() {
        super.onUndisplay();
        DataManager.getInstance().removeIconListener(this);
    }

    public boolean isDirty() { return false; }
    public boolean isEditable() { return false; }
    public boolean isFocusable() { return false; }
    public boolean isPasteable() { return false; }
    public boolean isSelectable() { return false; }

    public int getPreferredWidth() { return ICON_WIDTH; }
    public int getPreferredHeight() { return ICON_HEIGHT; }

    protected void layout(int width, int height) {
        setExtent(ICON_WIDTH, ICON_HEIGHT);
    }

    protected void paint(Graphics graphics) {
        if (_img == null)
            return;
        graphics.drawImage(0, 0,
            Math.min(getWidth(), _img.getWidth()),
            Math.min(getHeight(), _img.getHeight()),
            _img, 0, 0, 0
        );
    }

    //

    private void invokeInvalidate() {
        UiApplication.getUiApplication().invokeLater(new Runnable() {
            public void run() { invalidate(); }
        });
    }

    // ImageDataListener

    public void imageLoaded(long requestID, String url, EncodedImage image) {
        if (!StringUtilities.strEqual(_url, url))
            return;
        DataManager.getInstance().removeIconListener(this);
        _img = image;
        invokeInvalidate();
    }
}

