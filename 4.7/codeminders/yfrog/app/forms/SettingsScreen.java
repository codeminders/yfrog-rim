package codeminders.yfrog.app.forms;

import java.io.*;
import net.rim.device.api.ui.*;
import net.rim.device.api.ui.component.*;

import codeminders.yfrog.app.controls.*;
import codeminders.yfrog.data.*;
import codeminders.yfrog.res.*;
import codeminders.yfrog.utils.*;
import codeminders.yfrog.utils.ui.*;

public class SettingsScreen extends ScreenBase {

    private RichTextField _labelAccount;
    private ButtonField _btnViewAccount;
    private ObjectChoiceField _editMapsApp;
    private ObjectChoiceField _editImagesLocation;

    public SettingsScreen() {
        super(TITLE_SETTINGS);

        _labelAccount = new RichTextField(
            USE_ALL_WIDTH | NON_FOCUSABLE | RichTextField.TEXT_ALIGN_HCENTER);

        ButtonField btnLogin = new ButtonField(getRes().getString(MENU_CHANGE_ACCOUNT),
            ButtonField.CONSUME_CLICK | ButtonField.NEVER_DIRTY | USE_ALL_WIDTH | FIELD_HCENTER);
        btnLogin.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                changeAccount();
            }
        });

        _btnViewAccount = new ButtonField(" ",
            ButtonField.CONSUME_CLICK | ButtonField.NEVER_DIRTY | USE_ALL_WIDTH | FIELD_HCENTER);
        _btnViewAccount.setChangeListener(new FieldChangeListener() {
            public void fieldChanged(Field field, int context) {
                viewAccount();
            }
        });

        _editMapsApp = new ObjectChoiceField("", new Object[] {
            new IntStringContainer(Options.MapsApplication.INTERNAL,
                getRes().getString(LABEL_MAPS_APP_INTERNAL)),
            new IntStringContainer(Options.MapsApplication.GOOGLE_MAPS_WEB,
                getRes().getString(LABEL_MAPS_APP_GOOGLE_MAPS_WEB))
        }, 0, USE_ALL_WIDTH | FIELD_LEFT);

        _editImagesLocation = new ObjectChoiceField("", new Object[] {
            new IntStringContainer(Options.ImagesLocation.MEMORY,
                getRes().getString(LABEL_IMAGES_LOCATION_MEMORY)),
            new IntStringContainer(Options.ImagesLocation.SDCARD,
                getRes().getString(LABEL_IMAGES_LOCATION_SDCARD))
        }, 0, USE_ALL_WIDTH | FIELD_LEFT);

        add(new LabelField());
        add(_labelAccount);
        add(btnLogin);
        add(_btnViewAccount);
        add(new LabelField());
        add(new SeparatorField());
        add(new LabelField());
        add(new LabelField(getRes().getString(LABEL_MAPS_APPLICATION) + ":"));
        add(_editMapsApp);
        if (SysUtils.isSDCardAvaiable()) {
            add(new LabelField(getRes().getString(LABEL_IMAGES_LOCATION) + ":"));
            add(_editImagesLocation);
        }
    }

    protected void afterShow(boolean firstShow) {
        refreshData();

        setSelectedChoice(_editMapsApp,
            Options.getInstance().getMapsApplication());
        if (indexOfField(_editImagesLocation) >= 0)
            setSelectedChoice(_editImagesLocation,
                Options.getInstance().getImagesLocation());
        setDirty(false);
    }

    public void refreshData() {
        String username = Options.getInstance().getUsername();
        _labelAccount.setText(ResManager.getString(
            MSG_CURRENT_ACCOUNT_FMT, username));
        _btnViewAccount.setLabel(ResManager.getString(MENU_VIEW_USER_FMT,
            new Object[] { username }));
    }

    protected void makeFormMenu(Menu menu) {
        menu.add(new MenuItem(getRes(), MENU_CHANGE_ACCOUNT, 1000, 10) {
            public void run() { changeAccount(); }
        });
        menu.add(new MenuItem(getRes(), MENU_LOGOUT, 1000, 10) {
            public void run() { logout(); }
        });
        menu.add(new ViewUserMenuItem(Options.getInstance().getUsername()));
        menu.addSeparator();
        if (isDirty())
            menu.add(new MenuItem(getRes(), MENU_SAVE, 1000, 10) {
                public void run() { saveAndClose(); }
            });
        menu.add(new MenuItem(getRes(), MENU_CANCEL, 1000, 10) {
            public void run() { close(); }
        });
    }

    public void save() throws IOException {
        Options.getInstance().startChanges();
        try {
            Options.getInstance().setMapsApplication(
                ((IntStringContainer)_editMapsApp.getChoice(
                    _editMapsApp.getSelectedIndex())).intValue());
            if (indexOfField(_editImagesLocation) >= 0)
                Options.getInstance().setImagesLocation(
                    ((IntStringContainer)_editImagesLocation.getChoice(
                        _editImagesLocation.getSelectedIndex())).intValue());
        }
        finally {
            Options.getInstance().endChanges();
        }
        setDirty(false);
    }

    public boolean onClose() {
        if (!isDirty())
            return true;
        int res = UiUtils.ask(
            getRes().getString(MSG_SETTINGS_CHANGED),
            new String[] {
                getRes().getString(MENU_YES),
                getRes().getString(MENU_DISCARD),
                getRes().getString(MENU_CANCEL)
            },
            new int[] {
                Dialog.YES,
                Dialog.DISCARD,
                Dialog.CANCEL
            },
            Dialog.YES
        );
        switch (res) {
            case Dialog.YES:
                try {
                    save();
                    return true;
                }
                catch (Exception ex) {
                    UiUtils.alert(ex);
                }
                break;
            case Dialog.DISCARD:
                setDirty(false);
                return true;
        }
        return false;
    }

    //

    private void setSelectedChoice(ObjectChoiceField field, int value) {
        for (int i = field.getSize() - 1; i >= 0; i--) {
            IntStringContainer choice = (IntStringContainer)field.getChoice(i);
            if (choice.intValue() == value) {
                field.setSelectedIndex(i);
                break;
            }
        }
    }

    private void changeAccount() {
        LoginScreen.getInstance().show();
    }

    private void viewAccount() {
        new ViewUserScreen().show(Options.getInstance().getUsername());
    }

    private void logout() {
        Options.getInstance().resetAuth();
        LoginScreen.getInstance().showExclusive();
    }

    private void saveAndClose() {
        try {
            save();
            close();
        }
        catch (Exception ex) {
            UiUtils.alert(ex);
        }
    }
}

