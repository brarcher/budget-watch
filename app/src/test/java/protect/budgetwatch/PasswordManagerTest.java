package protect.budgetwatch;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 25)
public class PasswordManagerTest {

    private PasswordManager _pm;

    @Test
    public void noPasswordSet() {
        _pm = new PasswordManager(RuntimeEnvironment.application);
        assertTrue(!_pm.isPasswordEnabled());
    }

    @Test
    public void setPassword() {
        _pm = new PasswordManager(RuntimeEnvironment.application);
        _pm.setPassword("TestPassword1");
        assertTrue(_pm.isPasswordEnabled());
    }

    @Test
    public void checkIncorrectPassword() {
        _pm = new PasswordManager(RuntimeEnvironment.application);
        _pm.setPassword("TestPassword1");
        assertTrue(!_pm.isPasswordCorrect(""));
        assertTrue(!_pm.isPasswordCorrect("testPassword1"));
        assertTrue(!_pm.isPasswordCorrect("TestPassword"));
    }

    @Test
    public void checkCorrectPassword() {
        _pm = new PasswordManager(RuntimeEnvironment.application);
        _pm.setPassword("TestPassword1");
        assertTrue(_pm.isPasswordCorrect("TestPassword1"));
    }

    @Test
    public void clearPassword() {
        _pm = new PasswordManager(RuntimeEnvironment.application);
        _pm.setPassword("TestPassword1");
        assertTrue(_pm.isPasswordEnabled());

        _pm.clearPassword();
        assertTrue(!_pm.isPasswordEnabled());
    }

}
