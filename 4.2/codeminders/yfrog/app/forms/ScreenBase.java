package codeminders.yfrog.app.forms;

import java.util.Vector;
import net.rim.device.api.i18n.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.system.*;

import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.app.controls.*;

public class ScreenBase extends MainScreen implements
    YFrogResource, UiListener
{

    private static Vector _screens = null;

    protected Object _syncObj = new Object();
    protected UiApplication _app;
    private boolean _menuDisabled = false;
    private boolean _useCustomMenu = true;
    private boolean _addDefaultMenuItems = true;
    private boolean _showTitleSeparator = true;
    private boolean _showStatusSeparator = true;
    private boolean _modal = false;
    private boolean _leftRightMovementEnabled = false;
    private int _clientHeight = 0;
    private int _controlsPaddingRight = 0;
    /* VERSION_DEPENDED: 4.7 * /
    private int _lastOrientation = Display.getOrientation();
    /* */

    private ScreenBaseFieldManager _delegate;
    private Field _title = null;
    private Field _status = null;
    private Manager _controls;

    private Field _autoFocusField;
    protected boolean _firstShow = true;

    private boolean _listProgressVisible = false;
    private String _listNoProgressString = "";
    private Screen _progressScreen = null;

    public ScreenBase() {
        super(NO_VERTICAL_SCROLL | NO_HORIZONTAL_SCROLL);
        _delegate = new ScreenBaseFieldManager();
        super.add(_delegate);
        _delegate.setOwner(this);

        _app = UiApplication.getUiApplication();

        _controls = new VerticalFieldManager(
            VERTICAL_SCROLL | VERTICAL_SCROLLBAR | USE_ALL_WIDTH
        ) {
            protected void subpaint(Graphics graphics) {
                super.subpaint(graphics);
                afterSubpaintControls(graphics);
            }
        };

        //adjustInnerControls();
    }

    protected ScreenBase(int titleIndex) {
        this();
        if (titleIndex >= 0)
            setTitle(getRes().getString(titleIndex));
    }

    protected ResourceBundle getRes() {
        return ResManager.getResourceBundle();
    };

    // ScreenBaseFieldManager

    private static class ScreenBaseFieldManager extends VerticalFieldManager {

        private ScreenBase _owner = null;
        private int _sep1Y, _sep2Y, _sep3Y;

        public ScreenBaseFieldManager() {
            super(
                NO_VERTICAL_SCROLL | NO_HORIZONTAL_SCROLL |
                USE_ALL_WIDTH | USE_ALL_HEIGHT
            );
        }

        public void setOwner(ScreenBase owner) {
            _owner = owner;
        }

        private boolean validField(Field f) {
            if (f == null)
                return false;
            for (int i = getFieldCount() - 1; i >= 0; i--)
                if (getField(i) == f)
                    return true;
            return false;
        }

        private int getFieldX(Field f, int width) {
            int res;
            if (f.isStyle(FIELD_HCENTER))
                res = (width - f.getWidth()) / 2;
            else if (f.isStyle(FIELD_RIGHT))
                res = width - f.getWidth();
            else
                res = 0;
            return Math.max(0, res);
        }

        protected void sublayout(int width, int height) {
            if (_owner == null)
                return;

            width = Math.min(width, Display.getWidth());
            height = Math.min(height, Display.getHeight());

            int top = 0;
            int centerHeight = height;

            // Title
            boolean titleSeparator = false;
            if (validField(_owner._title)) {
                titleSeparator = true;
                layoutChild(_owner._title, width, centerHeight);
                int h = _owner._title.getHeight();
                setPositionChild(_owner._title,
                    getFieldX(_owner._title, width), 0);
                top += h;
                centerHeight -= h;
            }
            // Separator
            if (_owner._showTitleSeparator && titleSeparator) {
                _sep2Y = top;
                top += 1;
                centerHeight -= 1;
            }
            // Status
            if (validField(_owner._status)) {
                int border = _owner.getStatusVerticalBorderSize();
                layoutChild(_owner._status, width, centerHeight);
                int h = _owner._status.getHeight();
                setPositionChild(_owner._status,
                    getFieldX(_owner._status, width), height - h - border);
                centerHeight -= (h + (border * 2));
                // Separator
                if (_owner._showStatusSeparator) {
                    _sep3Y = (height - h - (border * 2)) - 1;
                    centerHeight -= 1;
                }
            }
            // Controls
            _owner._clientHeight = centerHeight;
            if (validField(_owner._controls)) {
                setPositionChild(_owner._controls, 0, top);
                layoutChild(_owner._controls,
                    width - _owner._controlsPaddingRight, centerHeight);
            }

            setExtent(width, height);
            _owner.afterLayout();

            /* VERSION_DEPENDED: 4.7 * /
            int orientation = Display.getOrientation();
            if (orientation != _owner._lastOrientation) {
                _owner._lastOrientation = orientation;
                _owner._app.invokeLater(new RunnableImpl(orientation) { public void run() {
                    _owner.onDisplayOrientationChanged(int0);
                }});
            }
            /* */
        }

        protected void subpaint(Graphics graphics) {
            if (_owner == null)
                return;

            int width = getWidth();
            int oldColor = graphics.getColor();
            // Title
            if (_owner._title != null) {
                int titleHeight = _owner._title.getHeight();
                int titleHeightTop = titleHeight / 2;
                int[] xInds = new int[] {0, width, width, width, 0, 0};
                int[] yInds = new int[] {0, 0, titleHeightTop, titleHeight, titleHeight, titleHeightTop };
                int[] cols = new int[]{ Color.LIGHTGREY, Color.LIGHTGREY, Color.WHITE, Color.LIGHTGREY, Color.LIGHTGREY, Color.WHITE };
                graphics.drawShadedFilledPath(xInds, yInds, null, cols, null);
                try { paintChild(graphics, _owner._title); } catch (Exception ex) { }
            }
            // Line
            if (_owner._showTitleSeparator && (_owner._title != null)) {
                graphics.setColor(Color.LIGHTSLATEGRAY);
                graphics.drawLine(0, _sep2Y, width, _sep2Y);
                graphics.setColor(oldColor);
            }
            // Controls
            paintChild(graphics, _owner._controls);
            if (_owner._status != null) {
                // Separator
                if (_owner._showStatusSeparator) {
                    graphics.setColor(Color.LIGHTSLATEGRAY);
                    graphics.drawLine(0, _sep3Y, width, _sep3Y);
                    graphics.setColor(oldColor);
                }
                // Status
                try { paintChild(graphics, _owner._status); } catch (Exception ex) { }
            }
            //super.subpaint(graphics);
        }
    }

    //

    public static ScreenBase[] getDisplayedScreens() {
        ScreenBase[] res;
        Vector list = getScreenList();
        synchronized (list) {
            res = new ScreenBase[list.size()];
            list.copyInto(res);
        }
        return res;
    }

    private static synchronized Vector getScreenList() {
        if (_screens == null)
            _screens = new Vector();
        return _screens;
    }

    protected void onDisplay() {
        //System.out.println("YFrog: " + getClass().getName() +  " displayed");
        super.onDisplay();

        Vector list = getScreenList();
        synchronized (list) {
            if (list.indexOf(this) < 0)
                list.addElement(this);
        }

        UiUtils.addListener(this);
        invokeAfterShow();
    }

    protected void onUndisplay() {
        //System.out.println("YFrog: " + getClass().getName() +  " closed");
        UiUtils.removeListener(this);
        super.onUndisplay();

        Vector list = getScreenList();
        synchronized (list) {
            list.removeElement(this);
        }

        if (_progressScreen != null) {
            try { _app.popScreen(_progressScreen); } catch (Exception ignored) {}
            _progressScreen = null;
        }

        _app.invokeLater(new Runnable() { public void run() { afterClose(); }});
    }

    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case Characters.ESCAPE:
                close();
                return true;
        }
        return super.keyChar(c, status, time);
    }

    public Manager getControlsManager() {
        return _controls;
    }

    protected void setControlsPaddingRight(int value) {
        _controlsPaddingRight = value;
        adjustInnerControls();
    }

    protected void afterSubpaintControls(Graphics graphics) {
    }

    public UiApplication getUiApplication() {
        return _app;
    }

    public boolean isActive() {
        return _app.getActiveScreen() == this;
    }

    public boolean isModal() {
        return _modal;
    }

    protected void setFocusLater(Field f) {
        if (f == null)
            return;
        _app.invokeLater(new RunnableImpl(this, f) { public void run() {
            try { ((Field)data1).setFocus(); } catch (Exception ignored) { }
            ((ScreenBase)data0).invalidate();
        }});
    }

    protected void afterLayout() { }

    /* VERSION_DEPENDED: 4.7 * /
    protected void onDisplayOrientationChanged(int orientation) { }
    /* */

    public int getClientHeight() {
        return _clientHeight;
    }

    protected void enableLeftRightMovement() {
        _leftRightMovementEnabled = true;
    }

    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        boolean res = super.navigationMovement(dx, dy, status, time);
        if (!_leftRightMovementEnabled)
            return res;
        if ((!res) && (dx != 0)) {
            if (super.navigationMovement(0, dx, status, time))
                return true;
        }
        return res;
    }

    // UiListener

    public void defaultFontChanged() {
        adjustInnerControls();
    }

    // Autofocus field

    public void setAutoFocusField(Field f) {
        _autoFocusField = f;
    }

    public Field getAutoFocusField() {
        return _autoFocusField;
    }

    public void updateAutoFocusField() {
        if (_autoFocusField == null)
            return;
        _app.invokeLater(new Runnable() { public void run() { updateAutoFocusFieldInt(); }});
    }

    private void updateAutoFocusFieldInt() {
        if (_autoFocusField == null)
            return;
        try { _autoFocusField.setFocus(); } catch (Exception ignored) {}
        invalidate();
    }

    // TitleLabelField

    private static class TitleLabelField extends LabelField {

        public TitleLabelField(String text) {
            super(text, USE_ALL_WIDTH | DrawStyle.HCENTER);
            updateFont();
        }

        public void updateFont() {
            setFont(UiUtils.getBoldFont());
        }
    }

    // Manager override

    public void add(Field field) { _controls.add(field); }
    public void insert(Field field, int index) { _controls.insert(field, index); }
    public void delete(Field field) { _controls.delete(field); }
    public void deleteAll() { _controls.deleteAll(); }
    public void deleteRange(int start, int count) { _controls.deleteRange(start, count); }
    public Field getField(int index) { return _controls.getField(index); }
    public int getFieldCount() { return _controls.getFieldCount(); }
    public Field getFieldWithFocus() { return _controls.getFieldWithFocus(); }
    public int getFieldWithFocusIndex() { return _controls.getFieldWithFocusIndex(); }

    public int indexOfField(Field f) {
        if (f == null)
            return -1;
        for (int i = getFieldCount() - 1; i >= 0; i--)
            if (getField(i) == f)
                return i;
        return -1;
    }

    // MainScreen stub

    public void setTitle(Field title) { setTitle((Object)title); }
    public void setTitle(ResourceBundleFamily family, int id) { setTitle((Object)family.getString(id)); }
    public void setTitle(String title) { setTitle((Object)title); }
    public void setStatus(Field status) { setStatus((Object)status); }

    // Menu

    public boolean onMenu(int instance) {
        if (_menuDisabled)
            return true;
        try {
            if (_useCustomMenu) {
                Menu menu = new Menu();
                makeMenu(menu, instance);
                if (menu.getSize() > 0)
                    menu.show();
                return true;
            }
            else
                return super.onMenu(instance);
        }
        catch (Exception ex) {
            UiUtils.alertMenuError(ex);
        }
        return true;
    }

    protected void makeContextMenu(Menu menu, int instance) {
        if (_menuDisabled)
            return;
        if (!_addDefaultMenuItems)
            return;
        // control-related menu
        if (_useCustomMenu)
            UiUtils.premakeMenu(menu);
        else
            super.makeMenu(menu, instance);
    }

    protected void makeMenu(Menu menu, int instance) {
        if (_menuDisabled)
            return;
        makeContextMenu(menu, instance);
        // form-related menu
        makeFormMenu(menu);
    }

    protected void disableMenu() {
        _menuDisabled = true;
    }

    protected void disableDefaultMenuItems() {
        _addDefaultMenuItems = false;
    }

    protected void makeFormMenu(Menu menu) {
    }

    protected final void addBackMenuItem(Menu menu, boolean defaultItem) {
        if (menu.getSize() > 0)
            menu.addSeparator();
        MenuItem mi = new MenuItem(getRes(), MENU_BACK, 1000, 10) {
            public void run() { close(); }
        };
        menu.add(mi);
        if (defaultItem)
            menu.setDefault(mi);
    }

    // threadsafe show/close

    public final void show() {
        _modal = false;
        if (Application.isEventDispatchThread())
            showInUI();
        else
            synchronized (Application.getEventLock()) { showInUI(); }
    }

    public final void showModal() {
        _modal = true;
        showInUI();
    }

    private void showInUI() {
        if (!onShow())
            return;
        adjustInnerControls();
        if (_app.getActiveScreen() == this)
            return;
        try { _app.popScreen(this); } catch (Exception ignored) { }
        if (_modal)
            _app.pushModalScreen(this);
        else
            _app.pushScreen(this);
    }

    public final void close() {
        if (!onClose())
            return;
        try {
            if (Application.isEventDispatchThread())
                doClose();
            else
                synchronized (Application.getEventLock()) { doClose(); }
        }
        catch (Exception ignored) {
        }
    }

    protected boolean onShow() {
        return true;
    }

    public boolean onClose() {
        return true;
    }

    protected void doClose() {
        super.close();
    }

    private final void invokeAfterShow() {
        _app.invokeLater(new Runnable() { public void run() {
            boolean b = _firstShow;
            _firstShow = false;
            afterShow(b);
            updateAutoFocusFieldInt();
        }});
    }

    protected void afterShow(boolean firstShow) { }

    protected void afterClose() { }

    // Title / Status

    private void adjustInnerControls() {
        Field focus = getLeafFieldWithFocus();

        _delegate.deleteAll();
        if (_title != null) {
            if (_title instanceof TitleLabelField)
                ((TitleLabelField)_title).updateFont();
            _delegate.add(_title);
        }
        _delegate.add(_controls);
        if (_status != null)
            _delegate.add(_status);

        UiUtils.setFocus(focus);
    }

    public Field getTitle() {
        return _title;
    }

    public void deleteTitle() {
        setTitle((Object)null);
    }

    public void setTitle(Object title) {
        if (_title != null) {
            Field tmp = _title;
            _title = null;
            _delegate.delete(tmp);
        }

        if (title == null)
            _title = null;
        else if (title instanceof Field)
            _title = (Field)title;
        else
            _title = new TitleLabelField(title.toString());

        adjustInnerControls();
    }

    public Field getStatus() {
        return _status;
    }

    public void setStatus(Object status) {
        if (_status != null) {
            Field tmp = _status;
            _status = null;
            _delegate.delete(tmp);
        }

        if (status == null)
            _status = null;
        else if (status instanceof Field)
            _status = (Field)status;
        else
            _status = new LabelField(status.toString());

        adjustInnerControls();
    }

    protected int getStatusVerticalBorderSize() {
        return 0;
    }

    protected void disableTitleSeparator() {
        _showTitleSeparator = false;
    }

    protected void disableStatusSeparator() {
        _showStatusSeparator = false;
    }

    // Progress

    private static class ProgressDialog extends Dialog {

        private Runnable _cancelCallback;

        public ProgressDialog(String message, Runnable cancelCallback
        ) {
            super(message,
                (cancelCallback != null) ? new Object[] { ResManager.getString(MENU_CANCEL) } : new Object[0],
                (cancelCallback != null) ? new int[] { CANCEL } : new int[0],
                (cancelCallback != null) ? CANCEL : 0,
                null
            );
            _cancelCallback = cancelCallback;
        }

        protected boolean keyChar(char c, int status, int time) {
            if (c == Characters.ESCAPE)
                return true;
            return super.keyChar(c, status, time);
        }

        protected void select() {
            int value = getSelectedValue();
            if ((value == Dialog.CANCEL) && (_cancelCallback != null))
                _cancelCallback.run();
        }
    }

    private VarRowHeightListField getList() {
        synchronized (Application.getEventLock()) {
            for (int i = getFieldCount() - 1; i >= 0; i--) {
                Field f = getField(i);
                if (f instanceof VarRowHeightListField)
                    return (VarRowHeightListField)f;
            }
        }
        return null;
    }

    public final void showProgress() {
        showProgress(getRes().getString(MSG_PROGRESS));
    }

    public void showProgress(String message) {
        showProgress(message, null);
    }

    public void showProgress(Runnable cancelCallback) {
        showProgress(getRes().getString(MSG_PROGRESS), cancelCallback);
    }

    public void showProgress(String message, Runnable cancelCallback) {
        VarRowHeightListField list = getList();
        synchronized (_syncObj) {
            if ((list != null) && (list.getSize() == 0)) {
                if (!_listProgressVisible) {
                    _listProgressVisible = true;
                    _listNoProgressString = list.getEmptyString();
                    synchronized (Application.getEventLock()) {
                        try { list.setEmptyString(message); } catch (Exception ignored) {}
                    }
                }
            }
            else {
                if (_progressScreen == null) {
                    _progressScreen = new ProgressDialog(message, cancelCallback);
                    synchronized (Application.getEventLock()) {
                        try { _app.pushScreen(_progressScreen); } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    public void hideProgress() {
        synchronized (_syncObj) {
            if (_progressScreen != null) {
                synchronized (Application.getEventLock()) {
                    try { _app.popScreen(_progressScreen); } catch (Exception ignored) {}
                }
                _progressScreen = null;
            }
            if (_listProgressVisible) {
                _listProgressVisible = false;
                VarRowHeightListField list = getList();
                if (list != null)
                    synchronized (Application.getEventLock()) {
                        try { list.setEmptyString(_listNoProgressString); } catch (Exception ignored) {}
                    }
            }
        }
    }

    // Data

    public void refreshDataIfVisible() {
        _app.invokeLater(new Runnable() { public void run() {
            if (isDisplayed())
                refreshData();
        }});
    }

    public void refreshData() {
    }
}

