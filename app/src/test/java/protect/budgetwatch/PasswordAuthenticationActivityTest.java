package protect.budgetwatch;

import android.app.Activity;
import android.widget.Button;
import android.widget.EditText;

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
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PasswordAuthenticationActivityTest {

    @Before
    public void setUp() {
        // Output logs emitted during tests so they may be accessed
        ShadowLog.stream = System.out;

        new PasswordManager(RuntimeEnvironment.application).setPassword("TestPassword1");
    }

    @Test
    public void testIncorrectPassword() {
        ActivityController controller = Robolectric.buildActivity(PasswordAuthenticationActivity.class).create();
        Activity activity = (Activity) controller.get();

        final EditText passwordText = activity.findViewById(R.id.password);
        final Button enterButton = activity.findViewById(R.id.enter_button);

        //Empty Password Field
        enterButton.performClick();
        assertEquals(passwordText.getError().toString(), RuntimeEnvironment.application.getString(R.string.error_field_required));
        assertTrue(!shadowOf(activity).isFinishing());
        //Incorrect Password
        passwordText.setText("Testpassword1");
        enterButton.performClick();

        assertEquals(passwordText.getError().toString(), RuntimeEnvironment.application.getString(R.string.error_incorrect_password));
        assertTrue(!shadowOf(activity).isFinishing());
    }

    @Test
    public void testCorrectPassword() {
        ActivityController controller = Robolectric.buildActivity(PasswordAuthenticationActivity.class).create();
        Activity activity = (Activity) controller.get();

        final EditText passwordText = activity.findViewById(R.id.password);
        final Button enterButton = activity.findViewById(R.id.enter_button);

        passwordText.setText("TestPassword1");
        enterButton.performClick();

        assertEquals(passwordText.getError(), null);
        assertTrue(shadowOf(activity).isFinishing());
    }
}
