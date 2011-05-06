package codeminders.yfrog.app.forms;

import javax.microedition.io.HttpConnection;

import codeminders.yfrog.app.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.twitter.*;
import codeminders.yfrog.twitter.items.*;
import codeminders.yfrog.lib.network.*;
import codeminders.yfrog.utils.ui.*;

public class LoginCheckScreen extends ScreenBase
    implements NetworkListener
{

    private long _loginRequestID = -1;

    public LoginCheckScreen() {
        super();
    }

    protected void onDisplay() {
        NetworkManager.getInstance().addListener(this);
        super.onDisplay();
    }

    protected void onUndisplay() {
        super.onUndisplay();
        NetworkManager.getInstance().removeListener(this);
    }

    protected void afterShow(boolean firstShow) {
        Authorization auth;
        if (Options.getInstance().isBasicAuthenticated())
            auth = new BasicAuthorization(
                Options.getInstance().getUsername(),
                Options.getInstance().getPassword()
            );
        else if (Options.getInstance().isOAuthAuthenticated())
            auth = new OAuthAuthorization(
                Options.getInstance().getTwitterAppConsumerKey(),
                Options.getInstance().getTwitterAppConsumerSecret(),
                Options.getInstance().getOAuthToken(),
                Options.getInstance().getOAuthTokenSecret()
            );
        else {
            showLoginAndClose();
            return;
        }
        _loginRequestID = TwitterManager.getInstance().accountVerifyCredentials(auth);
        showProgress(getRes().getString(MSG_LOGGING_IN));
    }

    //

    private void showHomeAndClose() {
        AppUtils.showHome();
        close();
    }

    private void showLoginAndClose() {
        LoginScreen.getInstance().showExclusive();
        close();
    }

    // NetworkListener

    public void started(long id) {
    }

    public void complete(long id, Object result) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
            if (result instanceof UserItemBase)
                Options.getInstance().setUsername(((UserItemBase)result).ScreenName);
            _app.invokeLater(new Runnable() { public void run() {
                showHomeAndClose();
            }});
        }
    }

    public void error(long id, Throwable ex) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
            if (
                (ex instanceof NetworkException)
                && (((NetworkException)ex).getResponseCode() == HttpConnection.HTTP_UNAUTHORIZED)
            )
                _app.invokeLater(new Runnable() { public void run() {
                    showLoginAndClose();
                }});
            else {
                UiUtils.alert(ex);
                /* */
                _app.invokeLater(new Runnable() { public void run() {
                    showLoginAndClose();
                }});
                /* * /
                YFrogApp.exit();
                /* */
            }
        }
    }

    public void cancelled(long id) {
        if (id == _loginRequestID) {
            _loginRequestID = -1;
            hideProgress();
            _app.invokeLater(new Runnable() { public void run() {
                showLoginAndClose();
            }});
        }
    }
}

