package protect.budgetwatch;

import android.app.Activity;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PasswordCreationActivityTest {
    private PasswordManager _pm;
    private Switch _enableSwitch;
    private EditText _passwordText;
    private EditText _confirmText;
    private Button _createButton;
    private final Context context = RuntimeEnvironment.application;

    @Before
    public void setUp() {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        _pm = new PasswordManager(RuntimeEnvironment.application);
    }

    private void InitiateTest() {
        ActivityController controller = Robolectric.buildActivity(PasswordCreationActivity.class).create();
        Activity activity = (Activity) controller.get();
        _enableSwitch = activity.findViewById(R.id.usePassword_switch);
        _passwordText = activity.findViewById(R.id.newPassword);
        _confirmText = activity.findViewById(R.id.confirmPassword);
        _createButton = activity.findViewById(R.id.createPassword_Button);
    }

    @Test
    public void testEmptyFields() {
        InitiateTest();

        assertTrue(!_enableSwitch.isChecked());
        assertEquals(_createButton.getText(), context.getString(R.string.createPasswordButton));

        _createButton.performClick();
        assertEquals(_passwordText.getError(), context.getString(R.string.error_field_required));

        _passwordText.setText("TestPassword1");
        _createButton.performClick();
        assertEquals(_confirmText.getError(), context.getString(R.string.error_field_required));
    }

    @Test
    public void testMisMatchedPasswords() {
        InitiateTest();

        _passwordText.setText("TestPassword1");
        _confirmText.setText("testPassword1");

        _createButton.performClick();
        assertEquals(_confirmText.getError(), context.getString(R.string.passwords_mismatch));
    }

    @Test
    public void testMatchedPasswords() {
        InitiateTest();

        _passwordText.setText("TestPassword1");
        _confirmText.setText("TestPassword1");

        _createButton.performClick();
        assertEquals(_passwordText.getError(), null);
        assertEquals(_confirmText.getError(), null);

        assertTrue(_pm.isPasswordEnabled());
        assertTrue(_pm.isPasswordCorrect("TestPassword1"));
        _pm.clearPassword();
    }

    @Test
    public void testPasswordAlreadyEnabled() {
        _pm.setPassword("TestPassword1");
        InitiateTest();

        assertTrue(_enableSwitch.isChecked());
        assertEquals(_createButton.getText(), context.getString(R.string.changePasswordButton));

        _pm.clearPassword();
    }

}
