package codeminders.yfrog.app;

import net.rim.blackberry.api.homescreen.*;
import net.rim.device.api.ui.*;

import codeminders.yfrog.data.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;
import codeminders.yfrog.app.forms.*;
import codeminders.yfrog.lib.network.*;

public class YFrogApp extends UiApplication {

    public static void main(String[] args) {
        initAppIcons();

        for (int i = 0; i < args.length; i++) {
            if ("devicestartup".equals(args[i])) {
                return;
            }
        }

        new YFrogApp().enterEventDispatcher();
    }

    private boolean _inPostActivate = false;
    private static boolean _iconsUpdated = false;

    public YFrogApp() {
        super();
        Options.initialize();
        DataManager.initialize();

        if (Options.getInstance().isAuthenticated())
            new LoginCheckScreen().show();
        else
            LoginScreen.getInstance().showExclusive();
    }

    public static void exit() {
        DataManager.deinitialize();
        System.exit(0);
    }

    private static void initAppIcons() {
        if (!HomeScreen.supportsIcons())
            return;

        AppIcons.Icon icon = AppIcons.getAppIcon(new AppIcons.AppIconResources(
            "icon64.png",
            "icon48.png",
            "icon36.png",
            "icon32.png",
            "icon28.png"
        ));
        try {
            HomeScreen.updateIcon(icon.normal);
            HomeScreen.setRolloverIcon(icon.rollover);
            _iconsUpdated = true;
        }
        catch (Exception ex) {
        }
    }

    // App activate

    public void activate() {
        if (SysUtils.inStartup())
            return;
        if (!_inPostActivate)
            invokeLater(new Runnable() { public void run() { postActivateApp(); }});
    }

    private void postActivateApp() {
        if (_inPostActivate)
            return;
        _inPostActivate = true;
        try {
            if (!_iconsUpdated)
                initAppIcons();
        }
        finally {
            _inPostActivate = false;
        }
    }
}

