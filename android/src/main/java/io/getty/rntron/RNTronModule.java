
package io.getty.rntron;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.google.protobuf.ByteString;

import org.tron.common.crypto.ECKey;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.Sha256Hash;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol.Transaction;

import java.security.SecureRandom;
import java.util.Arrays;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.MnemonicValidator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Validation.InvalidChecksumException;
import io.github.novacrypto.bip39.Validation.InvalidWordCountException;
import io.github.novacrypto.bip39.Validation.UnexpectedWhiteSpaceException;
import io.github.novacrypto.bip39.Validation.WordNotFoundException;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

import static io.github.novacrypto.bip32.Index.hard;

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

  private static byte[] _decode58Check(String input)
  {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4)
    { return null; }

    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(decodeData);
    byte[] hash1 = Sha256Hash.hash(hash0);
    if (hash1[0] == decodeCheck[decodeData.length] &&
            hash1[1] == decodeCheck[decodeData.length + 1] &&
            hash1[2] == decodeCheck[decodeData.length + 2] &&
            hash1[3] == decodeCheck[decodeData.length + 3])
    { return decodeData; }

    return null;
  }

  private static String _encode58Check(byte[] input)
  {
    byte[] hash0 = Sha256Hash.hash(input);
    byte[] hash1 = Sha256Hash.hash(hash0);
    byte[] inputCheck = new byte[input.length + 4];
    System.arraycopy(input, 0, inputCheck, 0, input.length);
    System.arraycopy(hash1, 0, inputCheck, input.length, 4);
    return Base58.encode(inputCheck);
  }


  @ReactMethod
  public void generateMnemonic(final Promise promise)
  {
    new Thread(new Runnable()
    {
      public void run()
      {
        try {
          StringBuilder mnemonic = new StringBuilder();
          byte[] entropy = new byte[Words.TWELVE.byteLength()];
          new SecureRandom().nextBytes(entropy);
          MnemonicGenerator generator = new MnemonicGenerator(English.INSTANCE);
          generator.createMnemonic(entropy, mnemonic::append);
          // System.out.println(mnemonic.toString());
          promise.resolve(mnemonic.toString());

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
          //Create ECKey from mnemonics seed
          byte[] mnemonicSeedBytes = new SeedCalculator().calculateSeed(mnemonics, "");

          //Derive private key
          ExtendedPrivateKey rootKey = ExtendedPrivateKey.fromSeed(mnemonicSeedBytes, Bitcoin.MAIN_NET);
          byte[] derivedKeyBytes = rootKey
                  .cKDpriv(hard(44))
                  .cKDpriv(hard(195))
                  .cKDpriv(hard(vaultNumber))
                  .extendedKeyByteArray();

          int keyOffset = derivedKeyBytes.length - 36;
          byte[] privateKeyBytes = Arrays.copyOfRange(derivedKeyBytes, keyOffset, keyOffset + 32);
          ECKey key = ECKey.fromPrivate(privateKeyBytes);

          //Get public address
          byte[] addressBytes = key.getAddress();
          String address = _encode58Check(addressBytes);

          //Get private key
          String privateKey = ByteArray.toHexString(privateKeyBytes).toUpperCase();

          //Get public key
          byte[] publicKeyBytes = key.getPubKey();
          String publicKey = ByteArray.toHexString(publicKeyBytes).toUpperCase();

          //Get password
          String password = new String(org.spongycastle.util.encoders.Base64.encode(privateKeyBytes));

          //Create generated account map
          WritableMap returnGeneratedAccount = Arguments.createMap();
          returnGeneratedAccount.putString("address", address);
          returnGeneratedAccount.putString("privateKey", privateKey);
          returnGeneratedAccount.putString("publicKey", publicKey);
          returnGeneratedAccount.putString("password", password);

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
  public void abstractSign(final String ownerPrivateKey, final String transaction, final Promise promise)
  {
    new Thread(new Runnable() {
      public void run() {
        try {
          System.out.println("Hello test from abstractSign");
        } catch(Exception e) {
          promise.reject("Failed to sign transaction", e.getMessage(), e);
        }
      }
    }).start();
  }

  public static Contract.TriggerSmartContract triggerCallContractWalletCli(byte[] address,
                                                                  byte[] contractAddress,
                                                                  long callValue, byte[] data, long tokenValue, String tokenId) {
    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(address));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(data));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
        builder.setCallTokenValue(tokenValue);
        builder.setTokenId(Long.parseLong(tokenId));
    }

    return builder.build();

  }

  @ReactMethod
  public static String triggerCallContract(String address,
                                          String contractAddress,
                                          String callValueStr, String data, String tokenValueStr, String tokenId) {

    byte[] addressBytes = _decode58Check(address);
    byte[] contractAddressBytes = _decode58Check(contractAddress);
    byte[] dataBytes = ByteArray.fromHexString(data);

    Long callValue = Long.parseLong(callValueStr);
    Long tokenValue = Long.parseLong(tokenValueStr);

    Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();

    builder.setOwnerAddress(ByteString.copyFrom(addressBytes));
    builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
    builder.setData(ByteString.copyFrom(dataBytes));
    builder.setCallValue(callValue);

    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }

    Contract.TriggerSmartContract response = builder.build();

    System.out.println(response);

    return ByteArray.toHexString(response.toByteArray());

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

          //Parse from hex encoded transaction
          byte[] transactionBytes = ByteArray.fromHexString(encodedTransaction);
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

          //Get hex encoded string of signed transaction
          byte[] signedTransactionBytes = transaction.toByteArray();
          String encodedSignedTransaction = ByteArray.toHexString(signedTransactionBytes).toUpperCase();

          //Return result
          promise.resolve(encodedSignedTransaction);
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
