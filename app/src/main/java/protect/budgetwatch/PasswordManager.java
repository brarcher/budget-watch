package protect.budgetwatch;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class PasswordManager {

    private final String PasswordKey = "password";
    private final int Iterations = 100;
    private final int KeyLength = 128;

    private SharedPreferences _prefs;

    PasswordManager(Context context) {
        _prefs = context.getSharedPreferences("protect.budgetwatch", Context.MODE_PRIVATE);
    }

    private String getStoredPasswordHash() {
        return _prefs.getString(PasswordKey, null);
    }

    public void setPassword(String password) {
        try {
            // Generate Salt
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            byte[] salt = new byte[KeyLength / 8];
            sr.nextBytes(salt);
            // Generate Hash
            byte[] hash = generateHash(password, salt);
            // Write to SharedPreferences
            _prefs.edit().putString(PasswordKey, toHex(salt) + ":" + toHex(hash)).commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void clearPassword() {
        _prefs.edit().remove(PasswordKey).commit();
    }

    boolean isPasswordEnabled() {
        return getStoredPasswordHash() != null;
    }

    boolean isPasswordCorrect(String password) {
        String storedPassword = getStoredPasswordHash();
        if (storedPassword == null) return false;

        String[] parts = storedPassword.split(":");
        byte[] salt = fromHex(parts[0]);
        byte[] hash = fromHex(parts[1]);

        byte[] passwordHash = generateHash(password, salt);

        int diff = hash.length ^ passwordHash.length;
        for (int i = 0; i < hash.length && i < passwordHash.length; i++) {
            diff |= hash[i] ^ passwordHash[i];
        }
        return diff == 0;
    }

    private byte[] generateHash(String password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, Iterations, KeyLength);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    private String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if (paddingLength > 0) {
            return String.format("%0" + paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

    private byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytes;
    }
}
