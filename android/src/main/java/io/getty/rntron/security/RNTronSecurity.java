package io.getty.rntron.security;

import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;

import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import io.getty.rntron.security.model.UserSecret;
import io.getty.rntron.security.module.Secrets;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class RNTronSecurity  {

    private static final String TAG = "RNTronSecurity";
    private final ReactApplicationContext reactContext;

//    private final RealmConfiguration realmConfig;
    private Realm realm;


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

    public void open(final String key) {
//        String androidId = Settings.Secure.getString(this.reactContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        RealmConfiguration realmConfig = new RealmConfiguration.Builder()
                .name("security.realm")
//                .encryptionKey(keyEncoded) TODO: implements encryptionKey
                .modules(new Secrets())
                .build();
        realm = Realm.getInstance(realmConfig);
    }

    public void close() {
        realm.close();
    }

}
