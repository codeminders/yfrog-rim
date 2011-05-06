package codeminders.yfrog.app.controls;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.res.*;

public class SearchField extends Manager implements YFrogResource {

    private static final int H_SPACE = 8;
    private static final int V_SPACE = 4;

    private LabelField _label;
    private EditField _edit;
    private ButtonField _button;

    public SearchField(int label) {
        super(NO_VERTICAL_SCROLL | NO_HORIZONTAL_SCROLL);

        _label = new LabelField(ResManager.getString(label) + ":");

        _edit = new EditField(EditField.NO_NEWLINE) {
            protected void onFocus(int direction) {
                super.onFocus(direction);
                UiApplication.getUiApplication().invokeLater(new Runnable() {
                    public void run() {
                        _edit.setCursorPosition(_edit.getText().length());
                    }
                });
            }
            protected boolean keyChar(char c, int status, int time) {
                switch (c) {
                    case Characters.ENTER:
                        onSearch();
                        return true;
                }
                return super.keyChar(c, status, time);
            }
        };

        _button = new ButtonField(ResManager.getString(MENU_GO),
            ButtonField.CONSUME_CLICK);
        _button.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) { onSearch(); }
        });

        add(_label);
        add(_edit);
        add(_button);
    }

    protected void sublayout(int width, int height) {
        if (getFieldCount() != 3)
            return;

        layoutChild(_label, width, height);
        layoutChild(_button, width, height);
        int editWidth = width - (H_SPACE * 4) - _label.getWidth() - _button.getWidth();
        layoutChild(_edit, editWidth, height);

        height = Math.max(_label.getHeight(), _button.getHeight());
        height = Math.max(height, _edit.getHeight());

        setPositionChild(_label,
            H_SPACE,
            V_SPACE + ((height - _label.getHeight()) / 2));
        setPositionChild(_edit,
            (H_SPACE * 2) + _label.getWidth(),
            V_SPACE + ((height - _edit.getHeight()) / 2));
        setPositionChild(_button,
            width - _button.getWidth() - H_SPACE,
            V_SPACE + ((height - _button.getHeight()) / 2));

        setExtent(width, height + (V_SPACE * 2));
    }

    protected void subpaint(Graphics graphics) {
        super.subpaint(graphics);

        int oldColor = graphics.getColor();
        graphics.setColor(Color.SILVER);
        graphics.drawRect(_edit.getLeft(), _edit.getTop(), _edit.getWidth(), _edit.getHeight());
        graphics.setColor(oldColor);
    }

    public EditField getEdit() {
        return _edit;
    }

    public String getText() {
        return _edit.getText().trim();
    }

    public void setText(String text) {
        if (text == null)
            text = "";
        _edit.setText(text);
        _edit.setCursorPosition(text.length());
    }

    protected void onSearch() { }
}

