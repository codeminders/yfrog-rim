package codeminders.yfrog.utils.ui;

import net.rim.device.api.system.Bitmap;
import net.rim.device.api.ui.Color;
import net.rim.device.api.ui.Graphics;
import net.rim.blackberry.api.homescreen.HomeScreen;

import codeminders.yfrog.utils.SysUtils;
import codeminders.yfrog.utils.StringUtils;

public class AppIcons {

    public static class Icon {
        public Bitmap normal;
        public Bitmap rollover;

        protected Icon(Bitmap normal, Bitmap rollover) {
            this.normal = normal;
            this.rollover = rollover;
        }
    }

    public static class AppIconResources {

        public String bmpName28 = null; public String bmpNameRollover28 = null;
        public String bmpName32 = null; public String bmpNameRollover32 = null;
        public String bmpName36 = null; public String bmpNameRollover36 = null;
        public String bmpName48 = null; public String bmpNameRollover48 = null;
        public String bmpName64 = null; public String bmpNameRollover64 = null;

        public AppIconResources(
            String bmpName28, String bmpNameRollover28,
            String bmpName32, String bmpNameRollover32,
            String bmpName36, String bmpNameRollover36,
            String bmpName48, String bmpNameRollover48,
            String bmpName64, String bmpNameRollover64
        ) {
            this.bmpName28 = bmpName28; this.bmpNameRollover28 = bmpNameRollover28;
            this.bmpName32 = bmpName32; this.bmpNameRollover32 = bmpNameRollover32;
            this.bmpName36 = bmpName36; this.bmpNameRollover36 = bmpNameRollover36;
            this.bmpName48 = bmpName48; this.bmpNameRollover48 = bmpNameRollover48;
            this.bmpName64 = bmpName64; this.bmpNameRollover64 = bmpNameRollover64;
        }

        public AppIconResources(
            String bmpName28,
            String bmpName32,
            String bmpName36,
            String bmpName48,
            String bmpName64
        ) {
            this.bmpName28 = bmpName28;
            this.bmpName32 = bmpName32;
            this.bmpName36 = bmpName36;
            this.bmpName48 = bmpName48;
            this.bmpName64 = bmpName64;
        }
    }

    public static Icon getAppIcon(AppIconResources res) {
        int w = 64;
        int h = 64;
        try {
            w = HomeScreen.getPreferredIconWidth();
            h = HomeScreen.getPreferredIconHeight();
        }
        catch (Exception ex) { }
        int size = (w < h) ? w : h;

        return getAppIcon(size, res);
    }

    private static Bitmap loadBitmap(String name) {
        if (StringUtils.isNullOrEmpty(name))
            return null;
        return Bitmap.getBitmapResource(SysUtils.currentModuleName(), name);
    }

    public static Icon getAppIcon(int preferredSize, AppIconResources res) {
        Bitmap bmp28  = loadBitmap(res.bmpName28);
        Bitmap bmpR28 = loadBitmap(res.bmpNameRollover28);
        Bitmap bmp32  = loadBitmap(res.bmpName32);
        Bitmap bmpR32 = loadBitmap(res.bmpNameRollover32);
        Bitmap bmp36  = loadBitmap(res.bmpName36);
        Bitmap bmpR36 = loadBitmap(res.bmpNameRollover36);
        Bitmap bmp48  = loadBitmap(res.bmpName48);
        Bitmap bmpR48 = loadBitmap(res.bmpNameRollover48);
        Bitmap bmp64  = loadBitmap(res.bmpName64);
        Bitmap bmpR64 = loadBitmap(res.bmpNameRollover64);

        Bitmap bmp, bmpR;
        if ((preferredSize >= 64) && (bmp64 != null))
            { bmp = bmp64; bmpR = bmpR64; }
        else if ((preferredSize >= 48) && (bmp48 != null))
            { bmp = bmp48; bmpR = bmpR48; }
        else if ((preferredSize >= 36) && (bmp36 != null))
            { bmp = bmp36; bmpR = bmpR36; }
        else if ((preferredSize >= 32) && (bmp32 != null))
            { bmp = bmp32; bmpR = bmpR32; }
        else
            { bmp = bmp28; bmpR = bmpR28; }

        if (bmp == null) {
            bmp = new Bitmap(16, 16);
            Graphics g = new Graphics(bmp);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, bmp.getWidth(), bmp.getHeight());
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, 16, 16);
            g.drawLine(0, 0, 15, 15);
            g.drawLine(0, 15, 15, 0);
        }
        if (bmpR == null)
            bmpR = bmp;

        return new Icon(bmp, bmpR);
    }
}
