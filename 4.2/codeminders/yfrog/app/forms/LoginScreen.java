package codeminders.yfrog.app.forms;

import java.util.*;
import net.rim.device.api.system.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;
import net.rim.device.api.ui.container.*;
import net.rim.device.api.util.*;

import codeminders.yfrog.app.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;

public class LoginScreen extends ScreenBase
    implements NetworkListener
{

    private static LoginScreen _instance = null;

    public static synchronized LoginScreen getInstance() {
        if (_instance == null)
            _instance = new LoginScreen();
        return _instance;
    }

    private static final String OAUTH_CALLBACK = "http://blackberry-yfrog-app.com/callback";

    private static final int TYPE_OAUTH = 0;
    private static final int TYPE_BASIC = 1;

    private static final int STAGE_CHECK_AUTH = 0;
    private static final int STAGE_OAUTH_REQUEST_TOKEN = 1;
    private static final int STAGE_OAUTH_ACCESS_TOKEN = 2;

    private ChoiceField _loginType;
    private Manager _loginPanel;

    private Manager _basicManager;
    private EditField _editUsername;
    private EditField _editPassword;
    private CheckboxField _boxRemember;

    private String _username = null;
    private String _password = null;
    private String _token = null;
    private String _tokenSecret = null;
    private boolean _remember = false;

    private long _loginRequestID = -1;
    private int _loginStage = -1;
    private boolean _authenticated = false;

    private LoginScreen() {
        super(TITLE_LOGIN);
        disableDefaultMenuItems();

        _loginType = new ObjectChoiceField("", new Object[] {
            getRes().getString(LABEL_LOGIN_OAUTH),
            getRes().getString(LABEL_LOGIN_BASIC)
        }, TYPE_OAUTH, USE_ALL_WIDTH | FIELD_LEFT);
        _loginType.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (context != FieldChangeListener.PROGRAMMATIC)
                    updateLoginPanel();
            }
        });

        _loginPanel = new VerticalFieldManager(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL);

        _editUsername = new EditField("", "", 200,
            EditField.JUMP_FOCUS_AT_END | EditField.NO_LEARNING
            | EditField.NO_NEWLINE | EditField.NO_COMPLEX_INPUT
        );
        _editPassword = new EditField("", "", 200,
            EditField.JUMP_FOCUS_AT_END | EditField.NO_LEARNING
            | EditField.NO_NEWLINE | EditField.NO_COMPLEX_INPUT
        );
        _boxRemember = new CheckboxField(getRes().getString(LABEL_REMEMBER_ME), false);

        ButtonField btnLogin = new ButtonField(getRes().getString(MENU_LOGIN),
            ButtonField.CONSUME_CLICK | USE_ALL_WIDTH | FIELD_HCENTER);
        btnLogin.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                if (canLogin())
                    login();
            }
        });

        _basicManager = new VerticalFieldManager(NO_HORIZONTAL_SCROLL | NO_VERTICAL_SCROLL) {
            protected void subpaint(Graphics graphics) {
                super.subpaint(graphics);
                int oldColor = graphics.getColor();
                graphics.setColor(Color.SILVER);
                graphics.drawRect(0, _editUsername.getTop(), getWidth(), _editUsername.getHeight());
                graphics.drawRect(0, _editPassword.getTop(), getWidth(), _editPassword.getHeight());
                graphics.setColor(oldColor);
            }
        };
        _basicManager.add(new LabelField(getRes().getString(LABEL_USERNAME) + ":"));
        _basicManager.add(_editUsername);
        _basicManager.add(new LabelField(getRes().getString(LABEL_PASSWORD) + ":"));
        _basicManager.add(_editPassword);
        _basicManager.add(new LabelField());

        add(new LabelField());
        add(new LabelField(getRes().getString(LABEL_LOGIN_TYPE) + ":"));
        add(_loginType);
        add(new LabelField());
        add(_loginPanel);
        add(_boxRemember);
        add(btnLogin);
        add(new LabelField());
        add(new BitmapField(Bitmap.getBitmapResource("about.png"),
            USE_ALL_WIDTH | FIELD_HCENTER));

        setAutoFocusField(_loginType);
    }

    public void showExclusive() {
        show();
        // close all other screens
        ScreenBase[] screens = getDisplayedScreens();
        for (int i = screens.length - 1; i >= 0; i--) {
            Screen scr = screens[i];
            if (scr == this)
                continue;
            if (scr instanceof TopScreenBase)
                ((TopScreenBase)scr).closeNoExit();
            else
                scr.close();
        }
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
        if (_instance == this)
            _instance = null;
    }

    protected void doClose() {
        if (_authenticated) {
            boolean homeFound = false;
            ScreenBase[] screens = getDisplayedScreens();
            if ((screens.length == 1) && (screens[0] == this))
                AppUtils.showHome();
            else
                for (int i = screens.length - 1; i >= 0; i--) {
                    if (screens[i] == this)
                        continue;
                    if (screens[i] instanceof ScreenBase)
                        ((ScreenBase)screens[i]).refreshData();
                }
            super.doClose();
        }
        else {
            ScreenBase[] screens = getDisplayedScreens();
            if ((screens.length == 1) && (screens[0] == this))
                YFrogApp.exit();
            else
                super.doClose();
        }
    }

    protected void afterShow(boolean firstShow) {
        if (Options.getInstance().isBasicAuthenticated()) {
            _loginType.setSelectedIndex(TYPE_BASIC);
            String username = Options.getInstance().getUsername();
            _editUsername.setText((username != null) ? username : "");
            String password = Options.getInstance().getPassword();
            _editPassword.setText((password != null)
                ? StringUtils.stringOfChar('*', password.length()) : "");
        }

        _boxRemember.setChecked(Options.getInstance().getRememberAuth());

        _editUsername.setCursorPosition(_editUsername.getText().length());
        _editPassword.setCursorPosition(_editPassword.getText().length());

        updateLoginPanel();
        setDirty(false);
    }

    protected void makeFormMenu(Menu menu) {
        MenuItem mi;

        if (canLogin()) {
            mi = new MenuItem(getRes(), MENU_LOGIN, 1000, 10) {
                public void run() { login(); }
            };
            menu.add(mi);
            menu.setDefault(mi);
        }

        menu.addSeparator();
        menu.add(new MenuItem(getRes(), MENU_CANCEL, 1000, 10) {
            public void run() { close(); }
        });
    }

    //

    private int getLoginType() {
        return _loginType.getSelectedIndex();
    }

    private void updateLoginPanel() {
        _loginPanel.deleteAll();
        if (getLoginType() == TYPE_BASIC) {
            _loginPanel.add(_basicManager);
            UiUtils.setFocus(_editUsername);
        }
    }

    private boolean canLogin() {
        return (getLoginType() != TYPE_BASIC) || (
            (_editUsername.getText().length() > 0)
            && (_editPassword.getText().length() > 0)
        );
    }

    private void login() {
        if (!canLogin())
            return;

        _remember = _boxRemember.getChecked();
        if (getLoginType() == TYPE_BASIC) {
            _username = _editUsername.getText();
            _password = _editPassword.isDirty()
                ? _editPassword.getText() : Options.getInstance().getPassword();
            _token = null;
            _tokenSecret = null;

            Authorization auth = new BasicAuthorization(_username, _password);
            _loginStage = STAGE_CHECK_AUTH;
            _loginRequestID = TwitterManager.getInstance().accountVerifyCredentials(auth);
            showProgress(getRes().getString(MSG_LOGGING_IN));
        }
        else if (getLoginType() == TYPE_OAUTH) {
            _username = null;
            _password = null;
            _token = null;
            _tokenSecret = null;

            _loginStage = STAGE_OAUTH_REQUEST_TOKEN;
            _loginRequestID = TwitterManager.getInstance().oauthRequestToken(
                Options.getInstance().getTwitterAppConsumerKey(),
                Options.getInstance().getTwitterAppConsumerSecret(),
                OAUTH_CALLBACK
            );
            showProgress(getRes().getString(MSG_LOGGING_IN));
        }
    }

    private void saveAndClose(UserItemBase user) {
        if (_token != null)
            Options.getInstance().setAuthOAuth(user.ScreenName, _token, _tokenSecret, _remember);
        else if (_password != null)
            Options.getInstance().setAuthBasic(user.ScreenName, _password, _remember);
        else
            return;
        close();
    }

    private void oauthRequestTokenReceived(ArgumentsList args) {
        String token = args.get("oauth_token");
        if (StringUtils.isNullOrEmpty(token)) {
            UiUtils.alert("Cannot retrieve OAuth Unauthorized Request Token");
            return;
        }
        String url = TwitterManager.getInstance().getAuthorizationUrl(token);
        _app.invokeLater(new RunnableImpl(url) { public void run() {
            WebBrowserScreen scr = new WebBrowserScreen() {
                protected boolean beforeNavigate(String url) {
                    if (url.startsWith(OAUTH_CALLBACK)) {
                        close();
                        oauthRequestAccessToken(url);
                        return false;
                    }
                    return true;
                }
            };
            scr.setTitle(getRes().getString(TITLE_LOGIN));
            scr.show((String)data0);
        }});
    }

    private void oauthRequestAccessToken(String url) {
        ArgumentsList args = new ArgumentsList();
        args.parseFromUrl(url);
        String token = args.get("oauth_token");
        String verifier = args.get("oauth_verifier");
        if (StringUtils.isNullOrEmpty(token) || StringUtils.isNullOrEmpty(verifier)) {
            UiUtils.alert("Error in Twitter response");
            return;
        }
        _loginStage = STAGE_OAUTH_ACCESS_TOKEN;
        _loginRequestID = TwitterManager.getInstance().oauthAccessToken(
            Options.getInstance().getTwitterAppConsumerKey(),
            Options.getInstance().getTwitterAppConsumerSecret(),
            token, verifier
        );
        showProgress(getRes().getString(MSG_LOGGING_IN));
    }

    private void oauthAccessTokenReceived(ArgumentsList args) {
        String token = args.get("oauth_token");
        String tokenSecret = args.get("oauth_token_secret");
        if (StringUtils.isNullOrEmpty(token) || StringUtils.isNullOrEmpty(tokenSecret)) {
            UiUtils.alert("Cannot retrieve OAuth Access Token");
            return;
        }
        _username = null;
        _password = null;
        _token = token;
        _tokenSecret = tokenSecret;

        Authorization auth = new OAuthAuthorization(
            Options.getInstance().getTwitterAppConsumerKey(),
            Options.getInstance().getTwitterAppConsumerSecret(),
            _token, _tokenSecret
        );
        _loginStage = STAGE_CHECK_AUTH;
        _loginRequestID = TwitterManager.getInstance().accountVerifyCredentials(auth);
        showProgress(getRes().getString(MSG_LOGGING_IN));
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
            switch (_loginStage) {
                case STAGE_CHECK_AUTH:
                    if (result instanceof UserItemBase) {
                        _authenticated = true;
                        saveAndClose((UserItemBase)result);
                    }
                    break;
                case STAGE_OAUTH_REQUEST_TOKEN:
                    if (result instanceof ArgumentsList)
                        oauthRequestTokenReceived((ArgumentsList)result);
                    break;
                case STAGE_OAUTH_ACCESS_TOKEN:
                    if (result instanceof ArgumentsList)
                        oauthAccessTokenReceived((ArgumentsList)result);
                    break;
            }
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
            UiUtils.alert(ex);
        }
    }

    public void cancelled(long id) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
        }
    }
}

