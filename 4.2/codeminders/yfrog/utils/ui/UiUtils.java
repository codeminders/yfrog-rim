package codeminders.yfrog.utils.ui;

import java.util.*;
import net.rim.device.api.math.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.utils.*;
import codeminders.yfrog.app.forms.*;

public class UiUtils {

    public static final int FONT_EXTRA_BOLD = 64;
    public static final int TOUCHSCREEN_MIN_VSPACING = 14;

    private static Object _syncObj = new Object();
    private static UiUtils _instance = null;

    private static UiUtils getInstance() {
        synchronized (_syncObj) {
            if (_instance == null)
                _instance = new UiUtils();
            return _instance;
        }
    }

    private Vector _listeners = new Vector();

    private UiUtils() {
        Application.getApplication().addGlobalEventListener(new GlobalEventListener() {
            public void eventOccurred(long guid, int data0, int data1, Object object0, Object object1) {
                if (guid == Font.GUID_FONT_CHANGED)
                    notifyDefaultFontChanged();
            }
        });
    }

    // Listeners

    public static void addListener(UiListener listener) {
        getInstance().addListenerInt(listener);
    }

    public static void removeListener(UiListener listener) {
        getInstance().removeListenerInt(listener);
    }

    private void addListenerInt(UiListener listener) {
        synchronized (_listeners) {
            _listeners.removeElement(listener);
            _listeners.addElement(listener);
        }
    }

    private void removeListenerInt(UiListener listener) {
        synchronized (_listeners) {
            _listeners.removeElement(listener);
        }
    }

    private void notifyDefaultFontChanged() {
        Vector v;
        synchronized (_listeners) { v = CloneableVector.clone(_listeners); }
        for (Enumeration e = v.elements(); e.hasMoreElements();)
            ((UiListener)e.nextElement()).defaultFontChanged();
    }

    /********************/
    /* Static Utilities */
    /********************/

    // Info

    public static Font getBoldFont() {
        return getBoldFont(Font.getDefault());
    }

    public static Font getBoldFont(Font font) {
        int style = font.getStyle();
        int boldFlag = ((style & (Font.BOLD | FONT_EXTRA_BOLD)) != 0) ? FONT_EXTRA_BOLD : Font.BOLD;
        style &= (~(Font.BOLD | FONT_EXTRA_BOLD));
        style |= boldFlag;
        return font.derive(style);
    }

    public static void setFocus(Field f) {
        if (f == null)
            return;
        UiApplication.getUiApplication().invokeLater(new RunnableImpl(f) {
            public void run() {
                try {
                    Field f = (Field)data0;
                    if (f.isFocusable())
                        f.setFocus();
                    UiApplication.getUiApplication().getActiveScreen().invalidate();
                }
                catch (Exception ignored) {}
            }
        });
    }

    public static void setCursorPosition(TextField f, int pos) {
        f.setCursorPosition((pos > f.getMaxSize()) ? f.getMaxSize() : pos);
    }

    public static boolean premakeMenu(Menu menu) {
        Field focus = UiApplication.getUiApplication().getActiveScreen()
            .getLeafFieldWithFocus();
        if (focus != null) {
            ContextMenu contextMenu = focus.getContextMenu();
            if (!contextMenu.isEmpty()) {
                menu.add(contextMenu, true);
                MenuItem defItem = contextMenu.getDefaultItem();
                if (defItem != null)
                    menu.setDefault(defItem);
                return true;
            }
        }
        return false;
    }

    // drawing

    public static void drawTiledImage(Graphics graphics, int x, int y, int width, int height, Bitmap bmp) {
        if ((bmp == null) || (width <= 0) || (height <= 0))
            return;
        int bmpW = bmp.getWidth();
        int bmpH = bmp.getHeight();
        for (int x1 = x, x2 = x + bmpW, xMax = x + width; x1 < xMax; x1 += bmpW, x2 += bmpW) {
            int w = (x2 <= xMax) ? bmpW : (xMax - x1);
            for (int y1 = y, y2 = y + bmpH, yMax = y + height; y1 < yMax; y1 += bmpH, y2 += bmpH) {
                int h = (y2 <= yMax) ? bmpH : (yMax - y1);
                graphics.drawBitmap(x1, y1, w, h, bmp, 0, 0);
            }
        }
    }

    public static void fillRect(Graphics graphics, XYRect rect, int color) {
        fillRect(graphics, rect.x, rect.y, rect.width, rect.height, color);
    }

    public static void fillRect(Graphics graphics, int x, int y, int width, int height, int color) {
        int oldColor = graphics.getColor();
        graphics.setColor(color);
        graphics.fillRect(x, y, width, height);
        graphics.setColor(oldColor);
    }

    // Alerts

    private static class DialogShowImpl implements Runnable {

        private String _msg;
        private boolean _keepForeground;
        private int _type;
        private String[] _buttons;
        private int[] _values;
        private int _defaultValue;
        private int _bitmap;
        private int _res = -1;

        public DialogShowImpl(Object msg, boolean keepForeground, int bitmap) {
            this(msg, -1, null, null, -1, keepForeground, bitmap);
        }

        public DialogShowImpl(Object msg, String[] buttons, int[] values,
            int defaultValue, boolean keepForeground, int bitmap
        ) {
            this(msg, -1, buttons, values, defaultValue, keepForeground, bitmap);
        }

        public DialogShowImpl(Object msg, int type, int defaultValue,
            boolean keepForeground, int bitmap
        ) {
            this(msg, type, null, null, defaultValue, keepForeground, bitmap);
        }

        private DialogShowImpl(Object msg, int type, String[] buttons, int[] values,
            int defaultValue, boolean keepForeground, int bitmap
        ) {
            if (msg == null)
                _msg = "null";
            else if (msg instanceof Throwable)
                _msg = StringUtils.getExceptionMessage((Throwable)msg);
            else
                _msg = msg.toString();

            _type = type;
            _buttons = buttons;
            _values = values;
            _defaultValue = defaultValue;
            _bitmap = bitmap;
            _keepForeground = keepForeground;
        }

        public void run() {
            UiApplication app = UiApplication.getUiApplication();
            Screen splash = null;
            boolean isBG = !app.isForeground();
            if (_keepForeground && isBG) {
                splash = new ScreenBase();
                app.pushScreen(splash);
                app.requestForeground();
            }

            Bitmap bmp = Bitmap.getPredefinedBitmap(_bitmap);
            Dialog dlg;
            if (_type >= 0)
                dlg = new Dialog(_type, _msg, _defaultValue, bmp, 0);
            else if (_buttons != null)
                dlg = new Dialog(_msg, _buttons, _values, _defaultValue, bmp);
            else
                dlg = new Dialog(Dialog.D_OK, _msg, Dialog.OK, bmp, 0);
            _res = dlg.doModal();

            if (_keepForeground && isBG)
                app.requestBackground();
            if (splash != null)
                app.popScreen(splash);
        }

        public int getRes() {
            return _res;
        }

        public Object getMessage() {
            return _msg;
        }
    }

    private static int showDialog(DialogShowImpl ds) {
        try {
            if (Application.isEventDispatchThread())
                ds.run();
            else
                UiApplication.getUiApplication().invokeAndWait(ds);
            return ds.getRes();
        }
        catch (Exception ex) {
            return Dialog.CANCEL;
        }
    }

    public static void info(Object message) {
        showDialog(new DialogShowImpl(message, true, Bitmap.INFORMATION));
    }

    public static void alert(Object message) {
        showDialog(new DialogShowImpl(message, true, Bitmap.EXCLAMATION));
    }

    public static int ask(int type, Object message, int defaultValue) {
        return showDialog(new DialogShowImpl(message, type, defaultValue, true, Bitmap.QUESTION));
    }

    public static int ask(Object message, String[] buttons, int[] values, int defaultValue) {
        return showDialog(new DialogShowImpl(message, buttons, values, defaultValue, true, Bitmap.QUESTION));
    }

    public static void alertMenuError(Throwable e) {
        if (e instanceof SecurityException)
            return;
        alert(e);
    }

    // wrap text

    private static int addWrappedLine(Vector lines, StringBuffer lineBuf, Font font,
        int calcWidth
    ) {
        // trim right
        int len = lineBuf.length();
        while ((len > 0) && (CharacterUtilities.isSpaceChar(lineBuf.charAt(len - 1))))
            len--;
        lineBuf.setLength(len);

        String line = lineBuf.toString();
        lines.addElement(line);
        return Math.max(calcWidth, font.getAdvance(line));
    }

    public static int wrapText(String value, Font font, int maxWidth, Vector lines) {
        int calcWidth = 0;
        lines.removeAllElements();

        int len = value.length();
        int start = 0;
        while (start < len) {
            int pos = value.indexOf('\n', start);
            if (pos < 0)
                pos = len;
            int end;
            for (end = pos; (end > start) && (value.charAt(end - 1) <= ' '); end--) { }
            String line = value.substring(start, end);
            if (line.length() == 0)
                lines.addElement("");
            else
                calcWidth = wrapTextLine(line, font, maxWidth, lines, calcWidth);
            start = pos + 1;
        }

        return calcWidth;
    }

    public static int wrapTextLine(String value, Font font, int maxWidth, Vector lines,
        int calcWidth
    ) {
        StringBuffer lineBuf = new StringBuffer();
        StringBuffer lineCheckBuf = new StringBuffer();
        int len = value.length();
        int start = 0;
        boolean isWrappedLine = false;
        while (start < len) {
            int pos;
            // find separator
            for (pos = start; pos < len; pos++) {
                char c = value.charAt(pos);
                if (CharacterUtilities.isLetter(c) || CharacterUtilities.isDigit(c))
                    continue;
                if (CharacterUtilities.isPunctuation(c)) {
                    pos++;
                    break;
                }
                break;
            }
            if (pos == start)
                pos++;
            // fetch word
            String word = value.substring(start, pos);
            start = pos;
            // check word
            lineCheckBuf.append(word);
            if (font.getAdvance(lineCheckBuf.toString()) <= maxWidth) {
                lineBuf.append(word);
            }
            else {
                if (lineBuf.length() > 0) {
                    calcWidth = addWrappedLine(lines, lineBuf, font, calcWidth);
                    lineBuf = new StringBuffer(word);
                    lineCheckBuf = new StringBuffer(word);

                    /*
                    // skip next leading spaces
                    while (
                        (start < len)
                        && (CharacterUtilities.isSpaceChar(value.charAt(start)))
                    )
                        start++;
                    */
                }
                else {
                    // too long line
                    lineBuf = lineCheckBuf;
                    while (
                        (lineBuf.length() > 0)
                        && (font.getAdvance(lineBuf.toString()) > maxWidth)
                    ) {
                        StringBuffer buf = new StringBuffer();
                        StringBuffer bufCheck = new StringBuffer();
                        for (int i = 0; i < lineBuf.length(); i++) {
                            char c = lineBuf.charAt(i);
                            bufCheck.append(c);
                            if (font.getAdvance(bufCheck.toString()) > maxWidth) {
                                calcWidth = addWrappedLine(lines, buf, font, calcWidth);
                                lineBuf.delete(0, i);
                                break;
                            }
                            buf.append(c);
                        }
                    }
                    lineCheckBuf = new StringBuffer(lineBuf.toString());
                }
                if (isWrappedLine) {
                    while ((lineBuf.length() > 0) && (CharacterUtilities.isSpaceChar(lineBuf.charAt(0))))
                        lineBuf.delete(0, 1);
                    while ((lineCheckBuf.length() > 0) && (CharacterUtilities.isSpaceChar(lineCheckBuf.charAt(0))))
                        lineCheckBuf.delete(0, 1);
                }
                isWrappedLine = true;
            }
        }
        if (lineBuf.length() > 0)
            calcWidth = addWrappedLine(lines, lineBuf, font, calcWidth);

        return calcWidth;
    }

    // Scale image

    private static EncodedImage scaleImage(EncodedImage img, int curSize, int newSize) {
        int numerator = Fixed32.toFP(curSize);
        int denominator = Fixed32.toFP(newSize);
        int scale = Fixed32.div(numerator, denominator);
        return img.scaleImage32(scale, scale);
    }

    public static EncodedImage zoomImage(EncodedImage img, int width, int height) {
        if (img == null)
            return null;

        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        if (
            ((imgWidth == width) && (imgHeight <= height))
            || ((imgHeight == height) && (imgWidth <= width))
        )
            return img;

        if (((double)imgWidth/(double)width) >= ((double)imgHeight/(double)height))
            return scaleImage(img, imgWidth, width);
        else
            return scaleImage(img, imgHeight, height);
    }
}

