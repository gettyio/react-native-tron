
package io.getty.rntron;

import com.alibaba.fastjson.JSONObject;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import org.tron.common.utils.Utils;
import org.tron.protos.Protocol;

import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.wordlists.English;

public class RNTronModule extends ReactContextBaseJavaModule {

  private static final int DECODED_PUBKEY_LENGTH = 21;
  private static final int MAINNET_PREFIX_BYTE = 0x41;
  private static final int TESTNET_PREFIX_BYTE = 0xa0;
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

  @ReactMethod
  public void generateMnemonic(final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try {
          String mnemonic = TronWallet.generateMnemonic();
          promise.resolve(mnemonic);
        } catch(Exception e) {
          //Exception, reject
          promise.reject("Failed to generate mnemonic seed", "Native exception thrown", e);
        }
      }
    }).start();
  }

  @ReactMethod
  public void validateMnemonic(final String mnemonic, final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try {
          MnemonicValidator
                  .ofWordList(English.INSTANCE)
                  .validate(mnemonic);
          promise.resolve("VALID");
        } catch (UnexpectedWhiteSpaceException e) {
          promise.reject("UnexpectedWhiteSpaceException", "UnexpectedWhiteSpaceException", e);
        } catch (InvalidWordCountException e) {
          promise.reject("InvalidWordCountException", "InvalidWordCountException", e);
        } catch (InvalidChecksumException e) {
          promise.reject("InvalidChecksumException", "InvalidChecksumException", e);
        } catch (WordNotFoundException e) {
          promise.reject("WordNotFoundException", "WordNotFoundException", e);
        } catch(Exception e) {
          promise.reject("Failed to validate mnemonic", "Native exception thrown", e);
        }
      }
    }).start();
  }

  @ReactMethod
  public void generateKeypair(final String mnemonics,
                              final int vaultNumber,
                              final boolean testnet,
                              final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try
        {
          String[] keypair = TronWallet.generateKeypair(mnemonics, vaultNumber, testnet);

          WritableMap generatedAccount = Arguments.createMap();
          generatedAccount.putString("address", keypair[0]);
          generatedAccount.putString("privateKey", keypair[1]);
          generatedAccount.putString("publicKey", keypair[2]);
          generatedAccount.putString("password", keypair[3]);

          promise.resolve(generatedAccount);
        }
        catch(Exception e)
        {
          promise.reject("Failed to generate account", "Native exception thrown", e);
        }
      }
    }).start();
  }

  @ReactMethod
  public void addressFromPk(final String ownerPrivateKey, final Promise promise)
  {
    new Thread(new Runnable() {
      public void run() {
        try {
          String address = TronWallet.addressFromPk(ownerPrivateKey);
          promise.resolve(address);
        } catch(Exception e) {
          promise.reject("Failed to sign transaction", e.getMessage(), e);
        }
      }
    }).start();
  }

    @ReactMethod
    public void signTransaction2(final String ownerPrivateKey, String transaction, final Promise promise)
    {
        new Thread(new Runnable() {
            public void run() {
                try {

                    Protocol.Transaction _transaction = TronWallet.packTransaction(transaction);
                    Protocol.Transaction _signedTransaction = TronWallet._sign(ownerPrivateKey, _transaction);
                    JSONObject result = Utils.printTransactionToJSON(_signedTransaction, false);
                    promise.resolve(result.toJSONString());

                } catch(Exception e) {
                    System.out.println("Error: "+e.getMessage());
                    e.printStackTrace();
                    promise.reject("Failed to triggerCallContract transaction", e.getMessage(), e);
                }
            }
        }).start();
    }

  @ReactMethod
  public void buildTriggerSmartContract(final String payload, final Promise promise)
  {
    new Thread(new Runnable() {
      public void run() {
        try {

          JSONObject unsignedTriggerSmartContractTransaction = TronWallet.buildTriggerSmartContract(payload);
          promise.resolve(unsignedTriggerSmartContractTransaction.toJSONString());

        } catch(Exception e) {
          //System.out.println("Error: "+e.getMessage());
          e.printStackTrace();
          promise.reject("Failed to triggerCallContract transaction", e.getMessage(), e);
        }
      }
    }).start();
  }

  @ReactMethod
  public void signTransaction(final String ownerPrivateKey, final String encodedTransaction, final Promise promise)
  {
    new Thread(new Runnable() {
      public void run() {
        try {
          String encoded = TronWallet._sign(ownerPrivateKey, encodedTransaction);
          promise.resolve(encoded);
        } catch(Exception e) {
          promise.reject("Failed to sign transaction", e.getMessage(), e);
        }
      }
    }).start();
  }
}
