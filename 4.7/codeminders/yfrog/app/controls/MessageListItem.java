package codeminders.yfrog.app.controls;

import java.util.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.utils.ui.*;

public class MessageListItem extends VarRowHeightListField.Item {

    private static final int ICON_WIDTH = 48;
    private static final int ICON_HEIGHT = 48;
    private static final int H_SPACE = 8;
    private static final int V_SPACE = 4;

    private long _itemID;
    private String _itemUsername;
    private long _itemCreatedAt;
    private String _itemText;
    private String _itemImageUrl;

    private int _height = -1;
    private int _userWidth = -1;
    private int _dateWidth = -1;
    private String[] _userLines = null;
    private String[] _dateLines = null;
    private String[] _textLines = null;

    public MessageListItem(StatusItem status) {
        super(status);

        _itemID = status.ID;
        _itemUsername = ((status.User != null) && (status.User.ScreenName != null))
            ? status.User.ScreenName : "";
        _itemCreatedAt = status.CreatedAt;
        _itemText = status.Text;
        _itemImageUrl = ((status.User != null) && (status.User.ProfileImageUrl != null))
            ? status.User.ProfileImageUrl : "";
    }

    public MessageListItem(DirectMessageItem message) {
        super(message);

        _itemID = message.ID;
        _itemUsername = (message.SenderScreenName != null) ? message.SenderScreenName : "";
        _itemCreatedAt = message.CreatedAt;
        _itemText = message.Text;
        _itemImageUrl = ((message.Sender != null) && (message.Sender.ProfileImageUrl != null))
            ? message.Sender.ProfileImageUrl : "";
    }

    public MessageListItem(SearchResultItem searchResult) {
        super(searchResult);

        _itemID = searchResult.ID;
        _itemUsername = searchResult.FromUser;
        _itemCreatedAt = searchResult.CreatedAt;
        _itemText = searchResult.Text;
        _itemImageUrl = searchResult.ProfileImageUrl;
    }

    public long getItemID() {
        return _itemID;
    }

    public StatusItem getStatus() {
        return (StatusItem)getTag();
    }

    public DirectMessageItem getDirectMessage() {
        return (DirectMessageItem)getTag();
    }

    public SearchResultItem getSearchResult() {
        return (SearchResultItem)getTag();
    }

    private void calculate(int width) {
        if (_height >= 0)
            return;

        Font fontText = Font.getDefault();
        Font fontUser = UiUtils.getBoldFont(fontText);
        Font fontDate = fontText.derive(fontText.getStyle(), fontText.getHeight() * 2 / 3);

        int textWidth = width - H_SPACE * 3 - ICON_WIDTH;
        int textHeight = V_SPACE;
        Vector lines = new Vector();

        String dateText = DataManager.getInstance().formatDate(_itemCreatedAt);

        _userWidth = fontUser.getAdvance(_itemUsername);
        _dateWidth = fontDate.getAdvance(dateText);

        if ((_userWidth + _dateWidth + H_SPACE) <= textWidth) {
            _userWidth = textWidth - H_SPACE - _dateWidth;
            _userLines = new String[] { _itemUsername };
            _dateLines = new String[] { dateText };
        }
        else {
            _userWidth = (textWidth - H_SPACE) / 3 * 2;
            _dateWidth = (textWidth - H_SPACE) / 3;

            UiUtils.wrapText(_itemUsername, fontUser, _userWidth, lines);
            _userLines = new String[lines.size()];
            lines.copyInto(_userLines);

            UiUtils.wrapText(dateText, fontDate, _dateWidth, lines);
            _dateLines = new String[lines.size()];
            lines.copyInto(_dateLines);
        }
        textHeight += Math.max(
            fontUser.getHeight() * _userLines.length,
            fontDate.getHeight() * _dateLines.length
        );

        UiUtils.wrapText(_itemText, fontText, textWidth, lines);
        _textLines = new String[lines.size()];
        lines.copyInto(_textLines);
        textHeight += fontText.getHeight() * _textLines.length;

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
        int dateLeft = textLeft + _userWidth + H_SPACE;
        int linesLength;

        Font fontText = Font.getDefault();
        Font fontUser = UiUtils.getBoldFont(fontText);
        Font fontDate = fontText.derive(fontText.getStyle(), fontText.getHeight() * 2 / 3);

        Font oldFont = graphics.getFont();
        int oldColor = graphics.getColor();

        // background
        if ((!isFocus) && ((row % 2) == 1))
            UiUtils.fillRect(graphics, 0, top, width, height - 1, 0xe8e8e8);
        // icon
        EncodedImage img = null;
        if (_itemImageUrl.length() > 0)
            img = DataManager.getInstance().loadIcon(_itemImageUrl);
        if (img != null)
            graphics.drawImage(H_SPACE, top + V_SPACE,
                Math.min(img.getWidth(), ICON_WIDTH),
                Math.min(img.getHeight(), ICON_HEIGHT),
                img, 0, 0, 0
            );
        // user
        graphics.setFont(fontUser);
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        int yUser = top + V_SPACE;
        linesLength = _userLines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_userLines[i],
                textLeft, yUser,
                DrawStyle.LEFT | DrawStyle.TOP,
                _userWidth + 8
            );
            yUser += fontUser.getHeight();
        }
        // date
        graphics.setFont(fontDate);
        if (!isFocus)
            graphics.setColor(Color.GRAY);
        int yDate = top + V_SPACE;
        linesLength = _dateLines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_dateLines[i],
                dateLeft, yDate,
                DrawStyle.LEFT | DrawStyle.TOP,
                _dateWidth + 8
            );
            yDate += fontDate.getHeight();
        }
        // message
        graphics.setFont(fontText);
        if (!isFocus)
            graphics.setColor(Color.BLACK);
        int yText = Math.max(yUser, yDate) + V_SPACE;
        linesLength = _textLines.length;
        for (int i = 0; i < linesLength; i++) {
            graphics.drawText(_textLines[i],
                textLeft, yText,
                DrawStyle.LEFT | DrawStyle.TOP,
                textWidth + 8
            );
            yText += fontText.getHeight();
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
        _userWidth = -1;
        _dateWidth = -1;
        _userLines = null;
        _dateLines = null;
        _textLines = null;
    }
}

