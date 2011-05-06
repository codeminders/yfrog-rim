package codeminders.yfrog.app;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import net.rim.blackberry.api.browser.*;
import net.rim.blackberry.api.invoke.*;
import net.rim.blackberry.api.maps.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.app.forms.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.lib.*;

public class AppUtils implements YFrogResource {

    // Top menu

    public static void makeTopMenu(Menu menu) {
        /*
        if (menu.getSize() > 0)
            menu.addSeparator();

        menu.add(new MenuItem(ResManager.getResourceBundle(), MENU_TOP_VIEW, 1000, 10) {
            public void run() { showTopViewMenu(); }
        });
        */
        makeTopViewMenu(menu);

        if (menu.getSize() > 0)
            menu.addSeparator();
        menu.add(new MenuItem(ResManager.getResourceBundle(), MENU_EXIT, 1000, 10) {
            public void run() { YFrogApp.exit(); }
        });
    }

    private static void showTopViewMenu() {
        Menu menu = new Menu();
        makeTopViewMenu(menu);
        menu.show();
    }

    private static void makeTopViewMenu(Menu menu) {
        if (menu.getSize() > 0)
            menu.addSeparator();

        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_HOME, 1000, 10) {
            public void run() { showHome(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_MENTIONS, 1000, 10) {
            public void run() { showMentions(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_DIRECT_MESSAGES, 1000, 10) {
            public void run() { showDirectMessages(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_UNSENT, 1000, 10) {
            public void run() { showUnsent(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_FOLLOWERS, 1000, 10) {
            public void run() { showFollowers(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_FOLLOWING, 1000, 10) {
            public void run() { showFollowing(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_SEARCH, 1000, 10) {
            public void run() { showSearch(); }
        });
        menu.addSeparator();
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_SETTINGS, 1000, 10) {
            public void run() { showSettings(); }
        });
        menu.add(new MenuItem(ResManager.getResourceBundle(), TITLE_ABOUT, 1000, 10) {
            public void run() { showAbout(); }
        });
    }

    public static void showHome() { HomeListScreen.getInstance().show(); }
    public static void showMentions() { MentionsScreen.getInstance().show(); }
    public static void showDirectMessages() { DirectMessagesScreen.getInstance().show(); }
    public static void showUnsent() { UnsentScreen.getInstance().show(); }
    public static void showFollowers() { FollowersScreen.getInstance().show(); }
    public static void showFollowing() { FollowingScreen.getInstance().show(); }
    public static void showSearch() { SearchScreen.getInstance().show(); }
    public static void showSettings() { new SettingsScreen().show(); }
    public static void showAbout() { new AboutScreen().show(); }

    // Location

    public static void showLocation(double latitude, double longitude) {
        try {
            switch (Options.getInstance().getMapsApplication()) {
                case Options.MapsApplication.INTERNAL: {
                    MapView view = new MapView();
                    view.setLatitude((int)(latitude * 100000.0));
                    view.setLongitude((int)(longitude * 100000.0));
                    Invoke.invokeApplication(Invoke.APP_TYPE_MAPS, new MapsArguments(view));
                    break;
                }
                case Options.MapsApplication.GOOGLE_MAPS_WEB: {
                    String url = DataManager.generateGoogleMapsUrl(latitude, longitude);
                    Browser.getDefaultSession().displayPage(url);
                    break;
                }
            }
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
        }
    }

    // Pictures

    public static String getSaveImagesPath() {
        if (
            SysUtils.isSDCardAvaiable()
            && (Options.getInstance().getImagesLocation() == Options.ImagesLocation.SDCARD)
        )
            return "file:///SDCard/BlackBerry/pictures";
        else
            return "file:///store/home/user/pictures";
    }

    public static String getSaveVideosPath() {
        if (
            SysUtils.isSDCardAvaiable()
            && (Options.getInstance().getImagesLocation() == Options.ImagesLocation.SDCARD)
        )
            return "file:///SDCard/BlackBerry/videos";
        else
            return "file:///store/home/user/videos";
    }

    private static String getYFrogSaveName(String yFrogUrl) {
        String res = null;
        if (!StringUtils.isNullOrEmpty(yFrogUrl)) {
            res = YFrogConnector.getId(yFrogUrl);
            if (res.indexOf('/') >= 0)
                res = null;
        }
        if (res == null)
            res = "";
        else
            res += "_";
        res = "yfrog_" + res + Long.toString(System.currentTimeMillis(), 16);
        return res;
    }

    private static void prepareFile(String url, int pathError, int fileError) throws IOException {
        String path = FileUtils.getDirectory(url);
        FileConnection conn = null;
        try {
            conn = (FileConnection)Connector.open(path);
            if (!conn.exists())
                conn.mkdir();
        }
        catch (Exception ex) {
            throw new IOException(ResManager.getString(pathError));
        }
        finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }

        conn = null;
        try {
            conn = (FileConnection)Connector.open(url);
            if (conn.exists())
                throw new IOException(ResManager.getString(fileError));
        }
        finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    public static String prepareSaveYFrogVideo(String yFrogUrl, String videoFileUrl) throws IOException {
        String path = getSaveVideosPath() + "/" + getYFrogSaveName(yFrogUrl)
            + FileUtils.getExtension(videoFileUrl);
        prepareFile(path, MSG_VIDEO_PATH_NOT_EXISTS, MSG_VIDEO_FILE_EXISTS);
        return path;
    }

    public static String saveYFrogImage(String yFrogUrl, EncodedImage image) throws IOException {
        if (image == null)
            throw new IllegalArgumentException("image");

        String path = getSaveImagesPath() + "/" + getYFrogSaveName(yFrogUrl);
        switch (image.getImageType()) {
            case EncodedImage.IMAGE_TYPE_BMP: path += ".bmp"; break;
            case EncodedImage.IMAGE_TYPE_GIF: path += ".gif"; break;
            case EncodedImage.IMAGE_TYPE_JPEG: path += ".jpg"; break;
            case EncodedImage.IMAGE_TYPE_PNG: path += ".png"; break;
            case EncodedImage.IMAGE_TYPE_TIFF: path += ".tif"; break;
            case EncodedImage.IMAGE_TYPE_WBMP: path += ".wbmp"; break;
            default: path += ".png"; break;
        }

        prepareFile(path, MSG_IMAGE_PATH_NOT_EXISTS, MSG_IMAGE_FILE_EXISTS);

        FileConnection conn = null;
        OutputStream os = null;
        try {
            conn = (FileConnection)Connector.open(path);
            conn.create();
            os = conn.openOutputStream();
            byte[] data = image.getData();
            os.write(data);
            os.flush();
        }
        finally {
            if (os != null) try { os.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }

        return path;
    }
}

