package protect.budgetwatch;

import android.content.Context;
import android.content.SharedPreferences;


public class PasswordManager {

    private final String SharedPreferencesName = "protect.budgetwatch";
    private final String PasswordName = "password";

    private SharedPreferences _prefs;

    PasswordManager(Context context) {
        _prefs = context.getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE);
    }

    private String getPassword() {
        return _prefs.getString(PasswordName, null);
    }

    public void setPassword(String password) {
        _prefs.edit().putString(PasswordName, password).commit();
    }

    public void clearPassword() {
        _prefs.edit().remove(PasswordName).commit();
    }

    public boolean isPasswordEnabled() {
        return getPassword() != null;
    }

    public boolean isPasswordCorrect(String password) {
        return password.equals(getPassword());
    }
}
