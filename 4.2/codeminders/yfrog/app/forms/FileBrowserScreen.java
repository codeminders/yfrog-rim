package codeminders.yfrog.app.forms;

import java.io.*;
import java.util.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import net.rim.device.api.io.file.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;

public class FileBrowserScreen extends ScreenBase
    implements ImageDataListener, FileSystemJournalListener
{

    private static Bitmap _folderBitmap = null;
    private static Bitmap _folderUpBitmap = null;
    private static Bitmap _imageBitmap = null;
    private static Bitmap _videoBitmap = null;

    public static Bitmap getFolderBitmap() {
        if (_folderBitmap == null)
            _folderBitmap = Bitmap.getBitmapResource("folder.png");
        return _folderBitmap;
    }

    public static Bitmap getFolderUpBitmap() {
        if (_folderUpBitmap == null)
            _folderUpBitmap = Bitmap.getBitmapResource("folderup.png");
        return _folderUpBitmap;
    }

    public static Bitmap getImageBitmap() {
        if (_imageBitmap == null)
            _imageBitmap = Bitmap.getBitmapResource("image.png");
        return _imageBitmap;
    }

    public static Bitmap getVideoBitmap() {
        if (_videoBitmap == null)
            _videoBitmap = Bitmap.getBitmapResource("video.png");
        return _videoBitmap;
    }

    private FileBrowserCallback _callback;

    private FoldersList _foldersList;
    private FilesList _filesList;
    private LabelField _status;

    private boolean _isMonitoringMode = false;
    private long _nextUSN = -1;

    public FileBrowserScreen() {
        super(TITLE_FILE_BROWSE);
        _status = new LabelField();
        setStatus(_status);
    }

    public void show(FileBrowserCallback callback) {
        deleteAll();
        _callback = callback;

        _filesList = new FilesList() {
            protected void onFolderClick(String name) {
                if ("..".equals(name))
                    _foldersList.removeLastFolder();
                else
                    _foldersList.addFolder(name);
            }
            protected void onFileClick(String url) {
                open();
            }
            protected void onSelectedIndexChanged() {
                updateFileName();
            }
        };
        _foldersList = new FoldersList(_filesList);

        String path = AppUtils.getSaveImagesPath();
        if (FileUtils.isPathExists(path))
            try { _foldersList.setPath(path); } catch (Exception ignored) {}

        add(_foldersList);
        add(new SeparatorField());
        add(_filesList);

        setAutoFocusField(_filesList);
        show();
    }

    public void showMediaMonitor(FileBrowserCallback callback) {
        deleteAll();
        _callback = callback;

        _foldersList = null;

        _filesList = new FilesList() {
            protected void onFileClick(String url) {
                open();
            }
            protected void onSelectedIndexChanged() {
                updateFileName();
            }
        };

        add(_filesList);

        setAutoFocusField(_filesList);

        _nextUSN = FileSystemJournal.getNextUSN();
        _app.addFileSystemJournalListener(this);
        _isMonitoringMode = true;

        show();
    }

    protected void onDisplay() {
        super.onDisplay();
        DataManager.getInstance().addImageFileListener(this);
    }

    protected void onUndisplay() {
        super.onUndisplay();
        DataManager.getInstance().removeImageFileListener(this);
        if (_isMonitoringMode)
            _app.removeFileSystemJournalListener(this);
    }

    protected void afterShow(boolean firstShow) {
        updateFileName();
    }

    protected void makeFormMenu(Menu menu) {
        boolean isFilesFocused = (getLeafFieldWithFocus() == _filesList);
        if ((_foldersList != null) && _foldersList.canBack()) {
            addBackMenuItem(menu, true);
        }
        FileItem fi = _filesList.getSelectedItem();
        if (isFilesFocused && (fi != null) && (!"..".equals(fi.Url))) {
            MenuItem mi = new MenuItem(getRes(), MENU_OPEN, 1000, 10) {
                public void run() { open(); }
            };
            menu.add(mi);
            menu.setDefault(mi);
        }
        menu.add(new MenuItem(getRes(), MENU_CANCEL, 1000, 10) {
            public void run() { close(); }
        });
    }

    protected boolean keyChar(char c, int status, int time) {
        switch (c) {
            case Characters.ESCAPE:
                if ((_foldersList != null) && _foldersList.removeLastFolder())
                    return true;
                break;
        }
        return super.keyChar(c, status, time);
    }

    //

    private void updateFileName() {
        FileItem item = _filesList.getSelectedItem();
        _status.setText((item != null) ? item.toString() : "");
    }

    private void open() {
        FileItem fi = _filesList.getSelectedItem();
        if ("..".equals(fi.Url)) {
            if (_foldersList != null)
                _foldersList.removeLastFolder();
            return;
        }
        if (fi.IsDirectory) {
            if (_foldersList != null)
                _foldersList.addFolder(fi.Name);
            return;
        }
        close();
        if (_callback != null)
            _app.invokeLater(new RunnableImpl(_callback, fi.Url) {
                public void run() {
                    ((FileBrowserCallback)data0).fileSelected((String)data1);
                }
            });
    }

    private void invokeAddItem(String path) {
        _app.invokeLater(new RunnableImpl(path) {
            public void run() { _filesList.addItem((String)data0); }
        });
    }

    private void invokeRemoveItem(String path) {
        _app.invokeLater(new RunnableImpl(path) {
            public void run() { _filesList.removeItem((String)data0); }
        });
    }

    // FoldersList

    private static class FolderItem extends LabelField {

        private FoldersList _owner;

        public FolderItem(FoldersList owner, String name) {
            super(name.endsWith("/") ? name : (name + "/"), FOCUSABLE | ELLIPSIS);
            _owner = owner;
        }

        public String getName() {
            String name = getText();
            if ((name.length() > 1) && name.endsWith("/"))
                name = name.substring(0, name.length() - 1);
            return name;
        }

        protected boolean keyChar(char c, int status, int time) {
            switch (c) {
                case Characters.SPACE:
                case Characters.ENTER:
                    _owner.onClick(this);
                    return true;
            }
            return super.keyChar(c, status, time);
        }

        protected boolean navigationClick(int status, int time) {
            _owner.onClick(this);
            return true;
        }

        protected boolean trackwheelClick(int status, int time) {
            _owner.onClick(this);
            return true;
        }
    }

    private static class FoldersList extends FlowFieldManager {

        private FilesList _filesList = null;
        private String _path = null;

        public FoldersList(FilesList filesList) {
            super();
            _filesList = filesList;

            addFolder("/");
            _filesList.setFolder(getPath());
        }

        public void onClick(FolderItem f) {
            int count = getFieldCount();
            int index = -1;
            for (int i = 0; i < count; i++)
                if (getField(i) == f) {
                    index = i;
                    break;
                }
            if ((index < 0) || (index >= (count - 1)))
                return;
            index++;
            deleteRange(index, count - index);
            _path = null;
            _filesList.setFolder(getPath());
            UiUtils.setFocus(_filesList);
        }

        public void addFolder(String name) {
            _path = null;
            FolderItem item = new FolderItem(this, name);
            add(item);
            _filesList.setFolder(getPath());
            UiUtils.setFocus(_filesList);
        }

        public boolean removeLastFolder() {
            int count = getFieldCount();
            if (count <= 1)
                return false;
            _path = null;
            deleteRange(count - 1, 1);
            _filesList.setFolder(getPath());
            UiUtils.setFocus(_filesList);
            return true;
        }

        public boolean canBack() {
            return getFieldCount() > 1;
        }

        private String calculatePath() {
            if (getFieldCount() <= 1)
                return "";
            StringBuffer res = new StringBuffer();
            int size = getFieldCount();
            for (int i = 1; i < size; i++) {
                Field f = getField(i);
                if (f instanceof FolderItem) {
                    res.append(((FolderItem)getField(i)).getName());
                    res.append('/');
                }
            }
            return res.toString();
        }

        public void setPath(String path) throws IOException {
            String url = FileUtils.getPathUrl(path);
            FileConnection conn = (FileConnection)Connector.open(url);
            try {
                if (!(conn.exists() && conn.isDirectory()))
                    throw new IOException("Path not exists");
            }
            finally {
                try { conn.close(); } catch (Exception ignored) {}
            }
            _path = null;
            deleteAll();
            add(new FolderItem(this, "/"));
            String[] names = StringUtils.split(url.substring(8), '/');
            for (int i = 0; i < names.length; i++)
                if ((names[i] != null) && (names[i].length() > 0))
                    add(new FolderItem(this, names[i]));
            _filesList.setFolder(getPath());
            UiUtils.setFocus(_filesList);
        }

        public String getPath() {
            if (_path == null)
                _path = calculatePath();
            return _path;
        }
    }

    // FilesList

    private static class FileItem {

        public String Url;
        public String Name;
        public boolean IsDirectory;

        private Bitmap _defIcon;
        private Bitmap _icon = null;

        public FileItem(String url) {
            this(url, null);
        }

        public FileItem(String url, String name) {
            Url = url;
            if ("..".equals(url)) {
                Name = url;
                IsDirectory = true;
                _defIcon = getFolderUpBitmap();
            }
            else {
                Name = (name != null) ? name : FileUtils.getFilename(url);
                _defIcon = null;
                try {
                    FileConnection conn = (FileConnection)Connector.open(url);
                    try {
                        IsDirectory = conn.isDirectory();
                        if (IsDirectory)
                            _defIcon = getFolderBitmap();
                        else if (isImageFile())
                            _defIcon = getImageBitmap();
                        else if (isVideoFile())
                            _defIcon = getVideoBitmap();
                    }
                    finally {
                        try { conn.close(); } catch (Exception ignored) {}
                    }
                }
                catch (Exception ex) {
                }
            }
        }

        public boolean isImageFile() {
            return FileUtils.isImageFile(Name);
        }

        public boolean isVideoFile() {
            return FileUtils.isVideoFile(Name);
        }

        public boolean isValid() {
            return IsDirectory || isImageFile() || isVideoFile();
        }

        public String toString() {
            return Name;
        }

        public void setIcon(Bitmap icon) {
            _icon = icon;
        }

        public Bitmap getIcon() {
            if (_icon != null)
                return _icon;
            if (IsDirectory)
                return _defIcon;
            DataManager.getInstance().loadImageFile(Url);
            return _defIcon;
        }
    }

    private static class FileItemComparator implements Comparator {
        public FileItemComparator() { }
        public int compare(Object o1, Object o2) {
            FileItem f1 = (FileItem)o1;
            FileItem f2 = (FileItem)o2;
            if (f1.IsDirectory && (!f2.IsDirectory)) return -1;
            else if ((!f1.IsDirectory) && f2.IsDirectory) return 1;
            int res = f1.Name.toLowerCase().compareTo(f2.Name.toLowerCase());
            if (res == 0)
                res = f1.Name.compareTo(f2.Name);
            return res;
        }
    }

    private static class FilesList extends Field {

        private static final int SPACE = 4;

        private int iconWidth, iconHeight;

        private String _path = null;
        private FileItem[] _items = null;
        private boolean _layoutPerformed = false;
        private int _lastSelectedIndex = -1;
        private int _selectedIndex = -1;
        private boolean _showNames = false;

        public FilesList() {
            super();

            int displayWidth = Display.getWidth();
            int itemW = (displayWidth / 3) - (SPACE * 2);
            if (itemW < 64) itemW = 64;
            else if (itemW > 86) itemW = 86;
            int cols = displayWidth / (itemW + (SPACE * 2));

            iconWidth = (displayWidth / cols) - (SPACE * 2);
            iconHeight = iconWidth * 3 / 4;
        }

        public void setShowNames(boolean value) {
            _showNames = value;
            if (_layoutPerformed)
                updateLayout();
        }

        protected boolean keyChar(char c, int status, int time) {
            switch (c) {
                case Characters.SPACE:
                case Characters.ENTER:
                    onClick();
                    return true;
            }
            return super.keyChar(c, status, time);
        }

        protected boolean navigationClick(int status, int time) {
            onClick();
            return true;
        }

        protected boolean trackwheelClick(int status, int time) {
            onClick();
            return true;
        }

        private int getColsCount() {
            int cols = getWidth() / (iconWidth + (SPACE * 2));
            return Math.max(cols, 1);
        }

        private int getItemHeight() {
            return _showNames
                ? (iconHeight + Font.getDefault().getHeight() + (SPACE * 3))
                : (iconHeight + (SPACE * 2));
        }

        private void fireSelectedIndexChanged() {
            if (_lastSelectedIndex == _selectedIndex)
                return;
            _lastSelectedIndex = _selectedIndex;
            onSelectedIndexChanged();
        }

        protected void moveFocus(int x, int y, int status, int time) {
            if ((_items == null) || (_items.length == 0))
                return;
            int cols = getColsCount();
            int itemWidth = iconWidth + (SPACE * 2);
            int itemHeight = getItemHeight();
            int col = (x + itemWidth - 1) / itemWidth;
            int row = (y + itemHeight - 1) / itemHeight;
            int index = (row * cols) + col;
            if ((index >= 0) && (index < _items.length))
                _selectedIndex = index;
            invalidate();
            fireSelectedIndexChanged();
        }

        protected boolean navigationMovement(int dx, int dy, int status, int time) {
            if ((_items == null) || (_items.length == 0))
                return false;
            int cols = getColsCount();
            int oldSelectedIndex = _selectedIndex;
            if (dx < 0) _selectedIndex--;
            else if (dx > 0) _selectedIndex++;
            if (dy < 0) _selectedIndex -= cols;
            else if (dy > 0) _selectedIndex += cols;
            normalizeSelectedIndex();
            invalidate();
            fireSelectedIndexChanged();
            boolean res = _selectedIndex != oldSelectedIndex;
            if (res && _layoutPerformed)
                updateLayout();
            return res;
        }

        /*
        protected int moveFocus(int amount, int status, int time) {
            if ((_items == null) || (_items.length == 0))
                return amount;
            int oldSelectedIndex = _selectedIndex;
            _selectedIndex += amount;
            normalizeSelectedIndex();
            invalidate();
            fireSelectedIndexChanged();
            if (_layoutPerformed)
                updateLayout();
            return amount - (_selectedIndex - oldSelectedIndex);
        }
        */

        public boolean isDirty() { return false; }
        public boolean isEditable() { return false; }
        public boolean isFocusable() { return (_items != null) && (_items.length > 0); }
        public boolean isPasteable() { return false; }
        public boolean isSelectable() { return false; }

        protected void layout(int width, int height) {
            _layoutPerformed = true;
            if ((_items == null) || (_items.length == 0)) {
                setExtent(width, 0);
                return;
            }
            int cols = getColsCount();
            int rows = (_items.length + cols - 1) / cols;
            int itemHeight = getItemHeight();

            setExtent(width, rows * itemHeight);
            return;
        }

        protected void paint(Graphics graphics) {
            if (_items == null)
                return;
            Font font = Font.getDefault();
            graphics.setFont(font);
            int cols = getColsCount();
            int itemWidth = iconWidth + (SPACE * 2);
            int itemHeight = getItemHeight();
            int col = 0;
            int x = SPACE;
            int y = SPACE;
            for (int i = 0; i < _items.length; i++) {
                Bitmap bmp = _items[i].getIcon();
                if (bmp != null)
                    graphics.drawBitmap(
                        x + ((iconWidth - bmp.getWidth()) / 2),
                        y + ((iconHeight - bmp.getHeight()) / 2),
                        bmp.getWidth(), bmp.getHeight(), bmp, 0, 0
                    );
                if (_showNames) {
                    String text = _items[i].toString();
                    int textWidth = font.getAdvance(text);
                    int left = (textWidth < iconWidth) ? ((iconWidth - textWidth) / 2) : 0;
                    graphics.drawText(text,
                        x + left, y + iconHeight + SPACE,
                        DrawStyle.LEFT | DrawStyle.TOP | DrawStyle.ELLIPSIS,
                        iconWidth
                    );
                }
                col++;
                x += itemWidth;
                if (col >= cols) {
                    col = 0;
                    x = SPACE;
                    y += itemHeight;
                }
            }
        }

        public void getFocusRect(XYRect rect) {
            if ((_items == null) || (_items.length == 0) || (_selectedIndex < 0)) {
                rect.set(0, 0, 0, 0);
                return;
            }
            int cols = getColsCount();
            int itemWidth = iconWidth + (SPACE * 2);
            int itemHeight = getItemHeight();
            int col = _selectedIndex % cols;
            int row = _selectedIndex / cols;
            rect.set(col * itemWidth, row * itemHeight, itemWidth, itemHeight);
        }

        private void cancelLoadImages() {
            if ((_items != null) && (_items.length > 0))
                for (int i = _items.length - 1; i >= 0; i--)
                    if (!_items[i].IsDirectory)
                        DataManager.getInstance().cancelLoadImageFile(_items[i].Url);
        }

        protected void onUndisplay() {
            super.onUndisplay();
            cancelLoadImages();
        }

        public void setFolder(String path) {
            cancelLoadImages();
            try {
                SimpleSortingVector items = new SimpleSortingVector();
                items.setSortComparator(new FileItemComparator());
                items.setSort(true);

                if (StringUtils.isNullOrEmpty(path)) {
                    for (Enumeration e = FileSystemRegistry.listRoots(); e.hasMoreElements();) {
                        String root = e.nextElement().toString();
                        if (root.endsWith("/"))
                            root = root.substring(0, root.length() - 1);
                        if (root.length() > 0)
                            items.addElement(new FileItem("file:///" + root + "/", root));
                    }
                    _path = "";
                }
                else {
                    items.addElement(new FileItem(".."));
                    if (path.endsWith("/"))
                        path = path.substring(0, path.length() - 1);
                    FileConnection conn = (FileConnection)Connector.open("file:///" + path);
                    try {
                        if (!(conn.exists() && conn.isDirectory()))
                            throw new IOException();
                        for (Enumeration e = conn.list(); e.hasMoreElements();) {
                            String name = e.nextElement().toString();
                            String itemPath = "file:///" + path + "/" + name;
                            if (name.endsWith("/"))
                                name = name.substring(0, name.length() - 1);
                            FileItem item = new FileItem(itemPath, name);
                            if (item.isValid())
                                items.addElement(item);
                        }
                    }
                    finally {
                        try { conn.close(); } catch (Exception ignored) {}
                    }
                    _path = path;
                }
                if (items.size() == 0)
                    _items = null;
                else {
                    _items = new FileItem[items.size()];
                    items.copyInto(_items);
                }
            }
            catch (Exception ex) {
                _path = null;
                _items = null;
            }
            afterItemsChanged();
        }

        public void addItem(String url) {
            if (StringUtils.isNullOrEmpty(url))
                return;
            if (_items != null)
                for (int i = 0; i < _items.length; i++) {
                    if (url.equals(_items[i].Url))
                        return;
                }
            FileItem item = new FileItem(url);
            if (item.IsDirectory || (!item.isValid()))
                return;
            cancelLoadImages();
            if (_items == null)
                _items = new FileItem[] { item };
            else
                Arrays.add(_items, item);
            afterItemsChanged();
        }

        public void removeItem(String url) {
            if (StringUtils.isNullOrEmpty(url) || (_items == null))
                return;
            FileItem item = null;
            for (int i = 0; i < _items.length; i++) {
                if (url.equals(_items[i].Url)) {
                    item = _items[i];
                    break;
                }
            }
            if (item == null)
                return;
            cancelLoadImages();
            Arrays.remove(_items, item);
            afterItemsChanged();
        }

        private void afterItemsChanged() {
            _selectedIndex = 0;
            normalizeSelectedIndex();
            _lastSelectedIndex = _selectedIndex;

            if (_layoutPerformed)
                updateLayout();

            onSelectedIndexChanged();
        }

        private void normalizeSelectedIndex() {
            int itemsLength = (_items != null) ? _items.length : 0;
            if (_selectedIndex < 0)
                _selectedIndex = 0;
            if (_selectedIndex >= itemsLength)
                _selectedIndex = itemsLength - 1;
        }

        private void onClick() {
            if (_selectedIndex < 0)
                return;
            FileItem item = _items[_selectedIndex];
            if (item.IsDirectory)
                onFolderClick(item.Name);
            else
                onFileClick(item.Url);
        }

        public FileItem getSelectedItem() {
            return (_selectedIndex < 0) ? null : _items[_selectedIndex];
        }

        public void fileImageLoaded(String url, EncodedImage image) {
            try {
                for (int i = _items.length - 1; i >= 0; i--)
                    if (StringUtilities.strEqual(_items[i].Url, url)) {
                        EncodedImage img = UiUtils.zoomImage(image, iconWidth, iconHeight);
                        if (img != null) {
                            _items[i].setIcon(img.getBitmap());
                            invalidate();
                        }
                        break;
                    }
            }
            catch (Exception ex) {
            }
        }

        protected void onFolderClick(String name) { }
        protected void onFileClick(String url) { }
        protected void onSelectedIndexChanged() { }
    }

    // ImageDataListener

    public void imageLoaded(long requestID, String url, EncodedImage image) {
        _app.invokeLater(new RunnableImpl(url, image) { public void run() {
            _filesList.fileImageLoaded((String)data0, (EncodedImage)data1);
        }});
    }

    // FileSystemJournalListener

    public void fileJournalChanged() {
        if (!_isMonitoringMode)
            return;
        long nextUSN = FileSystemJournal.getNextUSN();
        for (long lookUSN = _nextUSN; lookUSN < nextUSN; lookUSN++) {
            FileSystemJournalEntry entry = FileSystemJournal.getEntry(lookUSN);
            if (entry == null) continue;
            String path = entry.getPath();
            if ((path == null) || (path.length() == 0)) continue;
            path = "file://" + path;
            switch (entry.getEvent()) {
                case FileSystemJournalEntry.FILE_ADDED:
                    invokeAddItem(path);
                    break;
                case FileSystemJournalEntry.FILE_DELETED:
                    invokeRemoveItem(path);
                    break;
                case FileSystemJournalEntry.FILE_RENAMED:
                    invokeRemoveItem("file://" + entry.getOldPath());
                    invokeAddItem(path);
                    break;
            }
        }
        _nextUSN = nextUSN;
    }
}

