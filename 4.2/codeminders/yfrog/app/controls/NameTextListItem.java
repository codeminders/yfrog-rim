package codeminders.yfrog.app.controls;

import java.util.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.data.items.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;

public class NameTextListItem extends VarRowHeightListField.Item {

    private static final int H_SPACE = 8;
    private static final int V_SPACE = 4;

    private String _name;
    private String _text;

    private int _height = -1;
    private String[] _nameLines = null;
    private String[] _textLines = null;

    public NameTextListItem(Object tag) {
        this(null, tag.toString(), tag);
    }

    public NameTextListItem(String name, Object tag) {
        this(name, tag.toString(), tag);
    }

    public NameTextListItem(String name, String text, Object tag) {
        super(tag);
        _name = name;
        _text = text;
    }

    private String getName() { return _name; }
    private String getText() { return _text; }

    private void calculate(int width) {
        if (_height >= 0)
            return;

        Font fontText = Font.getDefault();
        Font fontName = UiUtils.getBoldFont(fontText);
        int height = 0;
        int textWidth = width - (H_SPACE * 2);
        Vector lines = new Vector();

        if (StringUtils.isNullOrEmpty(_name))
            _nameLines = new String[0];
        else {
            UiUtils.wrapText(_name, fontName, textWidth, lines);
            _nameLines = new String[lines.size()];
            lines.copyInto(_nameLines);
            height += fontName.getHeight() * _nameLines.length;
        }

        if (StringUtils.isNullOrEmpty(_text))
            _textLines = new String[0];
        else {
            UiUtils.wrapText(_text, fontText, textWidth, lines);
            _textLines = new String[lines.size()];
            lines.copyInto(_textLines);
            height += fontText.getHeight() * _textLines.length;
        }

        if ((_nameLines.length > 0) && (_textLines.length > 0))
            height += V_SPACE;
        _height = Math.max(height, fontText.getHeight()) + (V_SPACE * 2) + 1;
    }

    public int getHeight(int width) {
        calculate(width);
        return _height;
    }

    public void paint(VarRowHeightListField list, Graphics graphics, int row, int top, int width, int height) {
        calculate(width);

        boolean isFocus = graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS);
        int textWidth = width - (H_SPACE * 2);

        Font fontText = Font.getDefault();
        Font fontName = UiUtils.getBoldFont(fontText);
        int linesLength;

        Font oldFont = graphics.getFont();
        int oldColor = graphics.getColor();

        // background
        if ((!isFocus) && ((row % 2) == 1))
            UiUtils.fillRect(graphics, 0, top, width, height - 1, 0xe8e8e8);

        int y = top + V_SPACE;
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        // name
        if (_nameLines.length > 0) {
            graphics.setFont(fontName);
            linesLength = _nameLines.length;
            for (int i = 0; i < linesLength; i++) {
                graphics.drawText(_nameLines[i],
                    H_SPACE, y,
                    DrawStyle.LEFT | DrawStyle.TOP,
                    textWidth + 8
                );
                y += fontName.getHeight();
            }
            y += V_SPACE;
        }
        // text
        if (_textLines.length > 0) {
            graphics.setFont(fontText);
            linesLength = _textLines.length;
            for (int i = 0; i < linesLength; i++) {
                graphics.drawText(_textLines[i],
                    H_SPACE, y,
                    DrawStyle.LEFT | DrawStyle.TOP,
                    textWidth + 8
                );
                y += fontText.getHeight();
            }
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
        _nameLines = null;
        _textLines = null;
    }
}

