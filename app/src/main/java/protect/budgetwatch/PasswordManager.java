package protect.budgetwatch;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.common.io.BaseEncoding;

import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class PasswordManager
{

    private final static String PasswordKey = "password";
    private final static int Iterations = 100;
    private final static int KeyLength = 128;

    private SharedPreferences _prefs;

    PasswordManager(Context context)
    {
        _prefs = context.getSharedPreferences("protect.budgetwatch", Context.MODE_PRIVATE);
    }

    private String getStoredPasswordHash()
    {
        return _prefs.getString(PasswordKey, null);
    }

    public void setPassword(String password)
    {
        try
        {
            // Generate Salt
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[KeyLength / 8];
            sr.nextBytes(salt);
            // Generate Hash
            byte[] hash = generateHash(password, salt);
            // Write to SharedPreferences
            _prefs.edit().putString(PasswordKey, BaseEncoding.base16().encode(salt) + ":" + BaseEncoding.base16().encode(hash)).commit();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    void clearPassword()
    {
        _prefs.edit().remove(PasswordKey).commit();
    }

    boolean isPasswordEnabled()
    {
        return getStoredPasswordHash() != null;
    }

    boolean isPasswordCorrect(String password)
    {
        String storedPassword = getStoredPasswordHash();
        if (storedPassword == null) return false;

        String[] parts = storedPassword.split(":");
        byte[] salt = BaseEncoding.base16().decode(parts[0]);
        byte[] hash = BaseEncoding.base16().decode(parts[1]);

        byte[] passwordHash = generateHash(password, salt);

        return Arrays.equals(hash, passwordHash);
    }

    private byte[] generateHash(String password, byte[] salt)
    {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, Iterations, KeyLength);
        try
        {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e)
        {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
