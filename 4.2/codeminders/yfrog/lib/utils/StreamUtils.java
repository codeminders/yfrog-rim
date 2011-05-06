package codeminders.yfrog.lib.utils;

import java.io.*;

public class StreamUtils {

    private static final int BUFFER_SIZE = 1024 * 10;

    public static void copyStreamContent(InputStream source, OutputStream dest) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int countByte;
        while ((countByte = source.read(buffer)) > -1)
            dest.write(buffer, 0, countByte);
    }

    public static byte[] readStreamContent(InputStream source) throws IOException {
        byte[] res = new byte[0];
        byte[] buf = new byte[BUFFER_SIZE];
        int size;
        while ((size = source.read(buf, 0, buf.length)) >= 0) {
            if (size == 0)
                continue;
            byte[] newRes = new byte[res.length + size];
            if (res.length > 0)
                System.arraycopy(res, 0, newRes, 0, res.length);
            System.arraycopy(buf, 0, newRes, res.length, size);
            res = newRes;
        }
        return res;
    }

    public static String readStreamContentToString(InputStream source, String encoding) throws IOException {
        StringBuffer res = new StringBuffer();
        InputStreamReader isr = new InputStreamReader(source, encoding);
        try {
            char[] buf = new char[BUFFER_SIZE];
            int size;
            while ((size = isr.read(buf)) >= 0)
                if (size > 0)
                    res.append(buf, 0, size);
        }
        finally {
            try { isr.close(); } catch (Exception ignored) {}
        }
        return res.toString();
    }

}
