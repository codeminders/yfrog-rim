package codeminders.yfrog.app.controls;

import net.rim.device.api.ui.*;

import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.ui.*;

public class LoadMoreListItem extends VarRowHeightListField.Item {

    private static final int V_SPACE = 4;

    private String _text;

    public LoadMoreListItem() {
        super(null);
        _text = ResManager.getString(YFrogResource.LABEL_LOAD_MORE);
    }

    public int getHeight(int width) {
        Font font = Font.getDefault();
        return font.getHeight() + V_SPACE * 2 + VarRowHeightListField.getVSpacing() + 1;
    }

    public void paint(VarRowHeightListField list, Graphics graphics, int row, int top, int width, int height) {
        boolean isFocus = graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS);
        Font font = Font.getDefault();

        int textWidth = font.getAdvance(_text);

        Font oldFont = graphics.getFont();
        int oldColor = graphics.getColor();

        // background
        if ((!isFocus) && ((row % 2) == 1))
            UiUtils.fillRect(graphics, 0, top, width, height - 1, 0xe8e8e8);
        // label
        graphics.setFont(font);
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        graphics.drawText(_text,
            (width - textWidth) / 2, top + ((height - font.getHeight()) / 2),
            DrawStyle.LEFT | DrawStyle.TOP,
            textWidth + 8
        );
        // separator
        UiUtils.fillRect(graphics, 0, top + height - 1, width, 1, Color.GRAY);

        graphics.setFont(oldFont);
        graphics.setColor(oldColor);
    }

    public void adjustFocusRect(XYRect rect) {
        rect.height--;
    }
}

