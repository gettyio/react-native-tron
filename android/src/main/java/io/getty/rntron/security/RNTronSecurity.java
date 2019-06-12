package io.getty.rntron.security;

import android.provider.Settings;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.getty.rntron.security.model.UserSecret;
import io.getty.rntron.security.module.Secrets;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import org.apache.commons.codec.binary.Base64;

public class RNTronSecurity  {

    private static final String TAG = "RNTronSecurity";
    private final ReactApplicationContext reactContext;
    private Realm realm;

    private static final int HASH_BYTES = 48;
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";


    public RNTronSecurity(final ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }


    public void add(final String pin, UserSecret account) {
        open(pin);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                UserSecret result = realm.copyToRealm(account);
                Log.d(TAG, result.toString());
            }
        });
    }

    private void open(final String key) {
        try {
            String androidId = Settings.Secure.getString(this.reactContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            final byte[] keyCode = getEncryptionKey(key, androidId);

            RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                .name("security.realm")
                .encryptionKey(keyCode)
                .modules(new Secrets())
                .build();
            realm = Realm.getInstance(realmConfig);

        } catch(NoSuchAlgorithmException e) {
            Log.d(TAG, e.getMessage());
        }
        catch (InvalidKeySpecException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void close() {
        realm.close();
    }

    private static byte[] getEncryptionKey(final String key, final String androidId) throws InvalidKeySpecException, NoSuchAlgorithmException {

        byte[] encodeId = encodedHash256(androidId);
        byte[] encodeKey = encodedHash256(key);


        String newKey = toHex(encodeKey);
        String hexEncodeId = toHex(encodeId);

        byte[] hash = pbkdf2(newKey.toCharArray(), hexEncodeId.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, HASH_BYTES);
        Base64 base64 = new Base64();
        String encodedString = new String(base64.encode(hash));
        return encodedString.getBytes();
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        return skf.generateSecret(spec).getEncoded();
    }

    private static byte[] encodedHash256 (final String originalString)  throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(originalString.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] array) {
        BigInteger bi = new BigInteger(1, array);
        String hex = bi.toString(16);
        int paddingLength = (array.length * 2) - hex.length();
        if(paddingLength > 0) {
            return String.format("%0"  +paddingLength + "d", 0) + hex;
        } else {
            return hex;
        }
    }

}
