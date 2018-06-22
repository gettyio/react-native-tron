
package io.getty.rntron;

import android.content.Context;
import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableArray;


import org.tron.protos.Contract;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.Vote;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Witness;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.SymmEncoder;
import org.tron.common.utils.*;

import org.spongycastle.util.encoders.*;

import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

import java.security.SecureRandom;
import java.math.BigInteger;
import java.util.*;
import java.lang.*;

public class RNTronModule extends ReactContextBaseJavaModule {

  private static final int DECODED_PUBKEY_LENGTH = 21;
  private static final int DECODED_PREFIX_BYTE = 0x41; //0xa0 for testnet
  private static final int PRIVATE_KEY_LENGTH = 64;

  private final ReactApplicationContext reactContext;

  public RNTronModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNTron";
  }

  private static byte[] _decode58Check(String input)
  {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4)
    { return null; }

    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Hash.sha256(decodeData);
    byte[] hash1 = Hash.sha256(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
        hash1[1] == decodeCheck[decodeData.length + 1] &&
        hash1[2] == decodeCheck[decodeData.length + 2] &&
        hash1[3] == decodeCheck[decodeData.length + 3])
    { return decodeData; }

    return null;
  }

  private static String _encode58Check(byte[] input)
  {
    byte[] hash0 = Hash.sha256(input);
    byte[] hash1 = Hash.sha256(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }

  @ReactMethod
  public void generateAccount(final String password, final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          //Create mnemonics
          final StringBuilder sb = new StringBuilder();
          byte[] entropy = new byte[Words.TWELVE.byteLength()];
          new SecureRandom().nextBytes(entropy);
          new MnemonicGenerator(English.INSTANCE)
          .createMnemonic(entropy, new MnemonicGenerator.Target()
          {
            @Override
            public void append(final CharSequence string)
            { sb.append(string); }
          });

          //Create ECKey from mnemonics seed
          String mnemonics = sb.toString();
          byte[] mnemonicSeedBytes = new SeedCalculator().calculateSeed(mnemonics, password);
          ECKey key = ECKey.fromSeed(mnemonicSeedBytes);

          //Get public address
          byte[] addressBytes = key.getAddress();
          String address = _encode58Check(addressBytes);

          //Get private key
          byte[] privateKeyBytes = key.getPrivKeyBytes();
          String privateKey = ByteArray.toHexString(privateKeyBytes).toUpperCase();

          //Create generated account map
          WritableMap returnGeneratedAccount = Arguments.createMap();
          returnGeneratedAccount.putString("address", address);
          returnGeneratedAccount.putString("privateKey", privateKey);
          returnGeneratedAccount.putString("mnemonics", mnemonics);

          //Return generated account map
          promise.resolve(returnGeneratedAccount);
        }
        catch(Exception e)
        {
          //Exception, reject
          promise.reject("Failed to generate account", "Native exception thrown", e);
        }
      }
    }).start();
  }

  @ReactMethod
  public void signTransaction(final String ownerPrivateKey, final String encodedTransaction, final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          //Get key
          byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
          ECKey ownerKey = ECKey.fromPrivate(ownerPrivateKeyBytes);

          //Parse transaction
          byte[] transactionBytes = org.spongycastle.util.encoders.Base64.decode(encodedTransaction);
          Transaction transaction = Transaction.parseFrom(transactionBytes);
          if(transaction == null)
          {
            //Problem creating transaction, reject and return
            promise.reject("Failed to sign transaction", "Decoder/parser error", null);
            return;
          }

          //Set timestamp and sign transaction
          transaction = TransactionUtils.setTimestamp(transaction);
          transaction = TransactionUtils.sign(transaction, ownerKey);

          //Get base64 encoded string of signed transaction
          byte[] signedTransactionBytes = transaction.toByteArray();
          String base64EncodedTransaction = new String(org.spongycastle.util.encoders.Base64.encode(signedTransactionBytes));

          //Return result
          promise.resolve(base64EncodedTransaction);
        }
        catch(Exception e)
        {
          //Exception, reject
          promise.reject("Failed to sign transaction", "Native exception thrown", e);
        }
      }
    }).start();
  }
}
