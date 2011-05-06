package codeminders.yfrog.app.controls;

import java.util.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.utils.ui.UiUtils;
import codeminders.yfrog.utils.StringUtils;
import codeminders.yfrog.utils.SysUtils;

public class VarRowHeightListField extends Field {

    private static int _vSpacing = -1;

    public static int getVSpacing() {
        if (_vSpacing < 0) {
            if (SysUtils.isTouchScreenSupported())
                _vSpacing = UiUtils.TOUCHSCREEN_MIN_VSPACING;
            else
                _vSpacing = 0;
        }
        return _vSpacing;
    }

    private int _rows = 0;
    private int _selectedRow = -1;
    private int _lastSelectedRow = -1;

    private int[] _tops = new int[0];
    private int[] _heights = new int[0];
    private int _calculatedRows = 0;

    private boolean _layoutPerformed = false;
    private int _lastWidth = -1;

    private Item[] _items;
    private String _emptyString = null;
    private String[] _emptyStringLines = null;

    public VarRowHeightListField() {
        super();
        clear();
    }

    // List

    public String getEmptyString() {
        return _emptyString;
    }

    public void setEmptyString(String value) {
        _emptyString = (value != null) ? value.trim() : null;
        if (_rows == 0)
            relayout();
    }

    public void relayout() {
        if (!_layoutPerformed)
            return;
        _lastWidth = -1;
        updateLayout();
    }

    public void add(Item[] items) {
        removeLastAdd(0, items);
    }

    public void set(Item[] items) {
        removeLastAdd(_rows, items);
    }

    public void removeLastAdd(int removeCount, Item[] items) {
        if (removeCount > _rows)
            removeCount = _rows;

        if (items == null)
            items = new Item[0];

        int oldRows = _items.length - removeCount;
        _rows = oldRows + items.length;
        _calculatedRows = Math.min(_calculatedRows, oldRows);

        Item[] newItems = new Item[_rows];
        System.arraycopy(_items, 0, newItems, 0, oldRows);
        if (items.length > 0)
            System.arraycopy(items, 0, newItems, oldRows, items.length);
        _items = newItems;

        int[] newTops = new int[_rows];
        System.arraycopy(_tops, 0, newTops, 0, _calculatedRows);
        _tops = newTops;

        int[] newHeights = new int[_rows];
        System.arraycopy(_heights, 0, newHeights, 0, _calculatedRows);
        _heights = newHeights;

        if (_selectedRow < 0)
            _selectedRow = 0;
        if (_selectedRow >= _rows)
            _selectedRow = _rows - 1;

        if (_layoutPerformed)
            updateLayout();

        notifySelectedRowChanged();
    }

    public void clear() {
        _items = new Item[0];
        _rows = _items.length;
        resetCalculatedRows();
        setSelectedIndex(0);
        if (_layoutPerformed)
            updateLayout();
    }

    public int getSize() {
        return _rows;
    }

    public void setSelectedIndex(int index) {
        _selectedRow = index;
        if (_selectedRow < 0)
            _selectedRow = 0;
        if (_selectedRow >= _rows)
            _selectedRow = _rows - 1;

        if (_layoutPerformed) {
            if (calculateLayoutHeight(getWidth()) >= 0)
                updateLayout();
            else
                invalidate();
        }

        notifySelectedRowChanged();
    }

    public int getSelectedIndex() {
        return ((_rows > 0) && (_selectedRow < _rows)) ? _selectedRow : -1;
    }

    public Item getSelectedItem() {
        return getItem(getSelectedIndex());
    }

    public Item[] getItems() {
        return _items;
    }

    public Item getItem(int index) {
        if ((_items == null) || (index < 0) || (index >= _items.length))
            return null;
        return _items[index];
    }

    private void notifySelectedRowChanged() {
        if (_selectedRow == _lastSelectedRow)
            return;
        _lastSelectedRow = _selectedRow;
        afterSelectedRowChanged();
    }

    protected void afterSelectedRowChanged() { }

    // Field

    public void invalidate() {
        super.invalidate();
    }

    public void getFocusRect(XYRect rect) {
        int selectedRow = getSelectedIndex();
        if (selectedRow < 0) {
            rect.set(0, 0, 0, 0);
            return;
        }
        calculateRow(selectedRow, getWidth());
        rect.set(0, _tops[selectedRow], getWidth(), _heights[selectedRow]);
        adjustFocusRect(selectedRow, rect);
    }

    public int getPreferredHeight() {
        return 0;
    }

    public int getPreferredWidth() {
        return Display.getWidth();
    }

    public boolean isFocusable() {
        return (_rows > 0);
    }

    public boolean isPasteable() {
        return false;
    }

    public boolean isSelectable() {
        return false;
    }

    protected void layout(int width, int height) {
        if (width != _lastWidth) {
            resetCalculatedRows();
            calculateRow(_rows - 1, width);
            calculateEmptyString(width);
        }
        _layoutPerformed = true;
        if (_rows > 0)
            height = calculateLayoutHeight(width);
        else
            height = ((_emptyStringLines != null) && (_emptyStringLines.length > 0))
                ? ((getFont().getHeight() * _emptyStringLines.length) + 16)
                : 0;
        setExtent(width, height);
        _lastWidth = width;
    }

    protected int moveFocus(int amount, int status, int time) {
        int res = moveRow(amount, status);

        if (_layoutPerformed) {
            int h = calculateLayoutHeight(getWidth());
            if (h >= 0)
                updateLayout();
        }

        return res;
    }

    protected void moveFocus(int x, int y, int status, int time) {
        int row = getRowAt(y);
        setSelectedIndex((row >= 0) ? row : 0);
    }

    protected boolean navigationMovement(int dx, int dy, int status, int time) {
        if ((dy != 0) && ((status & KeypadListener.STATUS_ALT) != 0)) {
            int res = 0;
            for (int i = 5; (i > 0) && (res == 0); i--)
                res = moveRow(sign(dy), 0);
            if (_layoutPerformed)
                updateLayout();
            return true;
        }
        return false;
    }

    protected void onFocus(int direction) {
        super.onFocus(direction);
        if (_rows <= 0)
            return;
        if (direction > 0)
            setSelectedIndex(0);
        else if (direction < 0)
            setSelectedIndex(_rows - 1);
        invalidate();
    }

    protected void paint(Graphics graphics) {
        if (_rows == 0) {
            int y = 8;
            int width = getWidth();
            int emptyStringLinesLength = _emptyStringLines.length;
            Font font = graphics.getFont();
            for (int i = 0; i < emptyStringLinesLength; i++) {
                String s = _emptyStringLines[i];
                graphics.drawText(s,
                    (width - font.getAdvance(s)) / 2, y,
                    DrawStyle.LEFT | DrawStyle.TOP
                );
                y += font.getHeight();
            }
            return;
        }

        if (_calculatedRows == 0)
            return;
        XYRect clipRect = graphics.getClippingRect();
        int width = getWidth();
        // find first row
        int firstRow = getCalculatedRowAt(clipRect.y);
        if (firstRow < 0)
            firstRow = 0;
        // find last row
        int lastRow = getCalculatedRowAt(clipRect.y + clipRect.height - 1);
        if (lastRow < 0)
            lastRow = _calculatedRows - 1;
        // paint
        for (int row = firstRow; row <= lastRow; row++)
            paintRow(graphics, row, _tops[row], width, _heights[row]);
    }

    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case Characters.SPACE:
            case Characters.ENTER:
                if (onClick())
                    return true;
        }
        return super.keyChar(c, status, time);
    }

    protected boolean navigationClick(int status, int time) {
        if (onClick())
            return true;
        return super.navigationClick(status, time);
    }

    protected boolean trackwheelClick(int status, int time) {
        if (onClick())
            return true;
        return super.trackwheelClick(status, time);
    }

    //

    public void getRowBounds(int row, XYRect rect) {
        if ((row < 0) || (row >= _rows)) {
            rect.set(0, 0, 0, 0);
            return;
        }
        int width = getWidth();
        calculateRow(row, width);
        rect.set(0, _tops[row], width, _heights[row]);
    }

    private static int sign(int value) {
        return (value == 0) ? 0 : ((value > 0) ? 1 : -1);
    }

    protected int moveRow(int amount, int status) {
        try {
            if (amount == 0)
                return 0;

            if ((_rows == 0) || (amount == 0))
                return amount;
            if (_selectedRow < 0)
                _selectedRow = -1;
            if ((_selectedRow < 0) && (amount < 0))
                return amount;

            int res = 0;
            int newSelected = _selectedRow + amount;
            if (newSelected < 0) {
                res = newSelected;
                newSelected = 0;
            }
            if (newSelected >= _rows) {
                res = newSelected - _rows + 1;
                newSelected = _rows - 1;
            }

            if ((newSelected < 0) || (newSelected >= _rows))
                return amount;

            _selectedRow = newSelected;

            return res;
        }
        finally {
            notifySelectedRowChanged();
        }
    }

    private int getCalculatedRowAt(int y) {
        for (int i = 0; i < _calculatedRows; i++) {
            if ((_tops[i] <= y) && ((_tops[i] + _heights[i]) > y))
                return i;
        }
        return -1;
    }

    private int getRowAt(int y) {
        int i = getCalculatedRowAt(y);
        if (i >= 0)
            return i;
        while (_calculatedRows < _rows) {
            int row = _calculatedRows;
            calculateRow(row, getWidth());
            if ((_tops[row] <= y) && ((_tops[row] + _heights[row]) > y))
                return row;
        }
        return -1;
    }

    private int nextRowTopToCalculate() {
        return (_calculatedRows > 0)
            ? _tops[_calculatedRows - 1] + _heights[_calculatedRows - 1]
            : 0;
    }

    private void resetCalculatedRows() {
        _tops = new int[_rows];
        _heights = new int[_rows];
        _calculatedRows = 0;
        for (int i = _items.length - 1; i >= 0; i--)
            _items[i].reset();
    }

    private void calculateRow(int row, int width) {
        calculateRow(row, width, Integer.MAX_VALUE);
    }

    private void calculateRow(int row, int width, int upToHeight) {
        if ((row < _calculatedRows) || (row < 0) || (row >= _rows))
            return;
        int top = nextRowTopToCalculate();
        while ((_calculatedRows <= row) && (top < upToHeight)) {
            int height = getRowHeight(_calculatedRows, width);
            _tops[_calculatedRows] = top;
            _heights[_calculatedRows] = height;
            top += height;
            _calculatedRows++;
        }
    }

    private int calculateLayoutHeight(int width) {
        if (_rows == 0)
            return 0;
        if (_calculatedRows < _rows) {
            int row = (_selectedRow >= 0) ? _selectedRow : 0;
            calculateRow(row, width);
            calculateRow(_rows - 1, width, _tops[row] + Display.getHeight() * 2);
        }
        return nextRowTopToCalculate();
    }

    private void calculateEmptyString(int width) {
        if (StringUtils.isNullOrEmpty(_emptyString)) {
            _emptyStringLines = null;
            return;
        }
        Vector lines = new Vector();
        UiUtils.wrapText(_emptyString, getFont(), width - 16, lines);
        _emptyStringLines = new String[lines.size()];
        lines.copyInto(_emptyStringLines);
    }

    private boolean onClick() {
        int index = getSelectedIndex();
        if (index < 0)
            return false;
        return onClick(_items[index]);
    }

    // Callbacks

    protected int getRowHeight(int row, int width) {
        return _items[row].getHeight(width);
    }

    protected void paintRow(Graphics graphics, int row, int top, int width, int height) {
        _items[row].paint(this, graphics, row, top, width, height);
    }

    protected void adjustFocusRect(int row, XYRect rect) {
        _items[row].adjustFocusRect(rect);
    }

    protected boolean onClick(VarRowHeightListField.Item item) {
        return false;
    }

    // Items

    public static abstract class Item {

        private Object _tag;

        protected Item(Object tag) {
            _tag = tag;
        }

        public Object getTag() {
            return _tag;
        }

        public abstract int getHeight(int width);
        public abstract void paint(VarRowHeightListField list, Graphics graphics, int row, int top, int width, int height);
        public void adjustFocusRect(XYRect rect) { }
        public void reset() { }
    }
}

