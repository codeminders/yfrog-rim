package codeminders.yfrog.app.controls;

import java.util.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.utils.ui.*;

public class TextListItem extends VarRowHeightListField.Item {

    private static final int H_SPACE = 8;
    private static final int V_SPACE = 4;

    private int _height = -1;
    private String[] _lines = null;

    public TextListItem(Object value) {
        super(value);
    }

    public String getText() {
        return getTag().toString();
    }

    private void calculate(int width) {
        if (_height >= 0)
            return;

        Font font = Font.getDefault();

        int textWidth = width - (H_SPACE * 2);
        Vector lines = new Vector();
        UiUtils.wrapText(getText(), font, textWidth, lines);
        _lines = new String[lines.size()];
        lines.copyInto(_lines);

        int textHeight = Math.max(
            font.getHeight() * _lines.length,
            font.getHeight()
        );
        _height = textHeight + (V_SPACE * 2) + 1;
    }

    public int getHeight(int width) {
        calculate(width);
        return _height;
    }

    public void paint(VarRowHeightListField list, Graphics graphics, int row, int top, int width, int height) {
        calculate(width);

        boolean isFocus = graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS);
        int textWidth = width - (H_SPACE * 2);

        Font font = Font.getDefault();

        Font oldFont = graphics.getFont();
        int oldColor = graphics.getColor();

        // background
        if ((!isFocus) && ((row % 2) == 1))
            UiUtils.fillRect(graphics, 0, top, width, height - 1, 0xe8e8e8);
        // text
        graphics.setFont(font);
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        int y = top + V_SPACE;
        int linesLength = _lines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_lines[i],
                H_SPACE, y,
                DrawStyle.LEFT | DrawStyle.TOP,
                textWidth + 8
            );
            y += font.getHeight();
        }
        // separator
        UiUtils.fillRect(graphics, 0, top + height - 1, width, 1, Color.GRAY);

        graphics.setFont(oldFont);
        graphics.setColor(oldColor);
    }

    public void adjustFocusRect(XYRect rect) {
        rect.height--;
    }

    public void reset() {
        _height = -1;
        _lines = null;
    }
}

