package codeminders.yfrog.app.controls;

import java.util.*;
import net.rim.device.api.i18n.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.ui.*;

public class UserListItem extends VarRowHeightListField.Item {

    private static final int ICON_WIDTH = 48;
    private static final int ICON_HEIGHT = 48;
    private static final int H_SPACE = 8;
    private static final int V_SPACE = 4;

    private int _height = -1;
    private String[] _screenNameLines = null;
    private String[] _nameLines = null;

    public UserListItem(UserItemBase user) {
        super(user);
    }

    public UserItemBase getUser() {
        return (UserItemBase)getTag();
    }

    private void calculate(int width) {
        if (_height >= 0)
            return;

        UserItemBase user = getUser();

        Font font = Font.getDefault();

        int textWidth = width - H_SPACE * 3 - ICON_WIDTH;
        int textHeight = V_SPACE;
        Vector lines = new Vector();

        UiUtils.wrapText(
            (user.ScreenName != null) ? user.ScreenName : "",
            font, textWidth, lines);
        _screenNameLines = new String[lines.size()];
        lines.copyInto(_screenNameLines);
        textHeight += font.getHeight() * _screenNameLines.length;

        UiUtils.wrapText(
            (user.Name != null) ? user.Name : "",
            font, textWidth, lines);
        _nameLines = new String[lines.size()];
        lines.copyInto(_nameLines);
        textHeight += font.getHeight() * _nameLines.length;

        _height = Math.max(ICON_HEIGHT, textHeight) + V_SPACE * 2 + 1;
    }

    public int getHeight(int width) {
        calculate(width);
        return _height;
    }

    public void paint(VarRowHeightListField list, Graphics graphics, int row, int top, int width, int height) {
        calculate(width);

        boolean isFocus = graphics.isDrawingStyleSet(Graphics.DRAWSTYLE_FOCUS);
        int textWidth = width - H_SPACE * 3 - ICON_WIDTH;
        int textLeft = H_SPACE * 2 + ICON_WIDTH;
        int linesLength;

        Font font = Font.getDefault();

        Font oldFont = graphics.getFont();
        int oldColor = graphics.getColor();

        UserItemBase user = getUser();

        // background
        if ((!isFocus) && ((row % 2) == 1))
            UiUtils.fillRect(graphics, 0, top, width, height - 1, 0xe8e8e8);
        // icon
        EncodedImage img = null;
        if ((user.ProfileImageUrl != null) && (user.ProfileImageUrl.length() > 0))
            img = DataManager.getInstance().loadIcon(user.ProfileImageUrl);
        if (img != null)
            graphics.drawImage(H_SPACE, top + V_SPACE,
                Math.min(img.getWidth(), ICON_WIDTH),
                Math.min(img.getHeight(), ICON_HEIGHT),
                img, 0, 0, 0
            );

        int y = top + V_SPACE;
        graphics.setFont(font);
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        // screen name
        linesLength = _screenNameLines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_screenNameLines[i],
                textLeft, y,
                DrawStyle.LEFT | DrawStyle.TOP,
                textWidth + 8
            );
            y += font.getHeight();
        }
        // screen name
        y += V_SPACE;
        linesLength = _nameLines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_nameLines[i],
                textLeft, y,
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
        _screenNameLines = null;
        _nameLines = null;
    }
}

