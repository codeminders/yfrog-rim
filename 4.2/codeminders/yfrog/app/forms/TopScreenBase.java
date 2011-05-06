package codeminders.yfrog.app.forms;

import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.app.controls.*;

public class TopScreenBase extends ScreenBase {

    private boolean _exitOnClose = true;

    protected TopScreenBase() {
        super();
        //setStatus(new LabelField("<<<<<<<>>>>>>>>\n<<< TOOLBAR >>>\n<<<<<<<>>>>>>>>", USE_ALL_WIDTH | FIELD_HCENTER));
    }

    protected TopScreenBase(int titleIndex) {
        super(titleIndex);
        //setStatus(new LabelField("<<<<<<<>>>>>>>>\n<<< TOOLBAR >>>\n<<<<<<<>>>>>>>>", USE_ALL_WIDTH | FIELD_HCENTER));
    }

    protected void doClose() {
        if (_exitOnClose)
            //YFrogApp.exit();
            _app.requestBackground();
        else
            super.doClose();
    }

    protected void afterShow(boolean firstShow) {
        _exitOnClose = true;
        if (firstShow)
            refreshData();
    }

    public void closeNoExit() {
        _firstShow = true;
        _exitOnClose = false;
        close();
    }
}

