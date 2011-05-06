package codeminders.yfrog.app.forms;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;

import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;

public class AboutScreen extends ScreenBase {

    public AboutScreen() {
        super(TITLE_ABOUT);
        disableDefaultMenuItems();

        add(new NullField(FOCUSABLE));
        add(new LabelField());
        add(new RichTextField(getAboutText(),
            USE_ALL_WIDTH | RichTextField.TEXT_ALIGN_HCENTER | NON_FOCUSABLE));
        add(new LabelField());
        add(new NullField(FOCUSABLE));
        add(new BitmapField(Bitmap.getBitmapResource("about.png"),
            USE_ALL_WIDTH | FIELD_HCENTER));
        add(new NullField(FOCUSABLE));
    }

    protected void makeFormMenu(Menu menu) {
        addBackMenuItem(menu, true);
    }

    //

    private static String getAboutText() {
        String text = ResManager.getString(TEXT_ABOUT);
        text = StringUtils.replaceString(text, "%APP%", ResManager.getString(APP_TITLE));
        text = StringUtils.replaceString(text, "%VER%", ResManager.getString(APP_VERSION));
        return text;
    }
}

