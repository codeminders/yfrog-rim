package codeminders.yfrog.utils;

import java.io.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;

public class FileUtils {

    public static String getFilename(String url) {
        String res;
        int pos = url.lastIndexOf('/');
        if (pos < 0)
            return url;
        else
            return url.substring(pos + 1);
    }

    public static String getExtension(String url) {
        int pos = url.lastIndexOf('.');
        if (pos < 0)
            return null;
        return url.substring(pos).toLowerCase();
    }

    public static String getDirectory(String url) {
        int pos = url.lastIndexOf('/');
        if (pos < 0)
            return url;
        return url.substring(0, pos);
    }

    public static boolean isImageFile(String url) {
        String ext = getExtension(url);
        if (StringUtils.isNullOrEmpty(ext))
            return false;
        return
            ".jpg".equals(ext)
            || ".jpeg".equals(ext)
            || ".png".equals(ext)
            || ".gif".equals(ext)
            || ".tiff".equals(ext)
            || ".tif".equals(ext)
            || ".bmp".equals(ext);
    }

    public static boolean isVideoFile(String url) {
        String ext = getExtension(url);
        if (StringUtils.isNullOrEmpty(ext))
            return false;
        return
            ".3gp".equals(ext)
            || ".mp4".equals(ext)
            || ".flv".equals(ext);
    }

    public static String getPathUrl(String path) {
        if (StringUtils.isNullOrEmpty(path))
            return null;
        String url = path;
        if (!url.startsWith("file://")) {
            if (!url.startsWith("/"))
                url = "/" + url;
            url = "file://" + url;
        }
        return url;
    }

    public static boolean isPathExists(String path) {
        if (StringUtils.isNullOrEmpty(path))
            return false;
        String url = getPathUrl(path);
        FileConnection conn = null;
        try {
            conn = (FileConnection)Connector.open(url);
            return conn.exists();
        }
        catch (Exception ex) {
            return false;
        }
        finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    public static void deleteFile(String url) throws IOException {
        FileConnection conn = null;
        try {
            conn = (FileConnection)Connector.open(url);
            if (conn.exists())
                conn.delete();
        }
        finally {
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }
}

