package io.getty.rntron;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DLSequence;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcClient;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.cryptohash.Keccak256;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonFormat;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.novacrypto.bip32.ExtendedPrivateKey;
import io.github.novacrypto.bip32.networks.Bitcoin;
import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.SeedCalculator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

import static io.github.novacrypto.bip32.Index.hard;

public class TronWallet {

    private static String FULL_NODE_URL = "grpc-fullnode.tronwallet.me:50051";
    private static String SOLIDITY_NODE_URL = "grpc-solidity-node.tronwallet.me:50061";
    private static String TRX_MESSAGE_HEADER = "\u0019TRON Signed Message:\n32";

    private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;  //default testnet
    private static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;
    public static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

    public static byte[] _decode58Check(String input)
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

    public static String _encode58Check(byte[] input)
    {
        byte[] hash0 = Hash.sha256(input);
        byte[] hash1 = Hash.sha256(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
    }


    public static boolean addressValid(byte[] address) {
        if (ArrayUtils.isEmpty(address)) {
            System.out.println("Warning: Address is empty !!");
            return false;
        }
        if (address.length != Constant.ADDRESS_SIZE / 2) {
            System.out.println("Warning: Address length need " + Constant.ADDRESS_SIZE + " but " + address.length
                            + " !!");
            return false;
        }
        if (address[0] != addressPreFixByte) {
            System.out.println("Warning: Address need prefix with " + addressPreFixByte + " but " + address[0] + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    public static String addressFromPk(final String privateKey) {
        byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(privateKey);
        ECKey key = ECKey.fromPrivate(ownerPrivateKeyBytes);
        return _encode58Check(key.getAddress());
    }

    public static String[] generateKeypair(final String mnemonics, final int vaultNumber, final boolean testnet) {
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

        return new String[]{address, privateKey, publicKey, password};
    }

    public static String generateMnemonic() {
        StringBuilder mnemonic = new StringBuilder();
        byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);
        MnemonicGenerator generator = new MnemonicGenerator(English.INSTANCE);
        generator.createMnemonic(entropy, mnemonic::append);
        return mnemonic.toString();
    }

//    public static Transaction sign(Transaction transaction, ECKey myKey) {
//        Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
//        byte[] hash = Sha256Hash.hash(transaction.getRawData().toByteArray());
//
//        ECDSASignature signature = myKey.sign(hash);
//        ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
//        transactionBuilderSigned.addSignature(bsSign);
//        transaction = transactionBuilderSigned.build();
//        return transaction;
//    }


//    static signString(message, privateKey, useTronHeader = true) {
//        message = message.replace(/^0x/,'');
//        const signingKey = new SigningKey(privateKey);
//        const messageBytes = [
//            ...toUtf8Bytes(useTronHeader ? TRX_MESSAGE_HEADER : ETH_MESSAGE_HEADER),
//            ...utils.code.hexStr2byteArray(message)
//        ];
//
//        const messageDigest = keccak256(messageBytes);
//        const signature = signingKey.signDigest(messageDigest);
//
//        const signatureHex = [
//        '0x',
//                signature.r.substring(2),
//                signature.s.substring(2),
//                Number(signature.v).toString(16)
//        ].join('');
//
//        return signatureHex
//    }

    public static ECKey.ECDSASignature decodeFromDER(byte[] bytes) {
        ASN1InputStream decoder = null;
        try {
            decoder = new ASN1InputStream(bytes);
            DLSequence seq = (DLSequence) decoder.readObject();
            if (seq == null) {
                throw new RuntimeException("Reached past end of ASN.1 " +
                        "stream.");
            }
            ASN1Integer r, s;
            try {
                r = (ASN1Integer) seq.getObjectAt(0);
                s = (ASN1Integer) seq.getObjectAt(1);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
            // OpenSSL deviates from the DER spec by interpreting these
            // values as unsigned, though they should not be
            // Thus, we always use the positive versions. See:
            // http://r6.ca/blog/20111119T211504Z.html
            return new ECKey.ECDSASignature(r.getPositiveValue(), s
                    .getPositiveValue());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (decoder != null) {
                try {
                    decoder.close();
                } catch (IOException x) {
                }
            }
        }
    }



    public static String signString(final String ownerPrivateKey, final String hexString, boolean needToHash) {
        try
        {

            String pk = ownerPrivateKey;

            byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(pk);

            byte[] message = ByteArray.fromHexString(hexString);
            byte[] headerBytes = TRX_MESSAGE_HEADER.getBytes("UTF-8");

            byte[] completeMessage = new byte[message.length + headerBytes.length];
            System.arraycopy(headerBytes, 0, completeMessage, 0, headerBytes.length);
            System.arraycopy(message, 0, completeMessage, headerBytes.length, message.length);

            Keccak256 digest =  new Keccak256();
            digest.update(completeMessage);
            byte[] messageDigest = digest.digest();

            System.out.println("HASH: "+ Hex.toHexString(messageDigest));

            ECKey _key = ECKey.fromPrivate(ownerPrivateKeyBytes);
            ECKey.ECDSASignature _signature =_key.sign(messageDigest);

            System.out.println("SIG R: "+Hex.toHexString(_signature.r.toByteArray()).length());
            System.out.println("SIG R: "+Hex.toHexString(_signature.r.toByteArray()));
            System.out.println("SIG S: "+Hex.toHexString(_signature.s.toByteArray()).length());
            System.out.println("SIG S: "+Hex.toHexString(_signature.s.toByteArray()));

            String hexR = Hex.toHexString(_signature.r.toByteArray());
            String hexS = Hex.toHexString(_signature.s.toByteArray());
            String hexV = Integer.toString(_signature.v, 16);

            if (hexR.length() == 66) {
                hexR = hexR.substring(2);
            }

            return hexR + hexS + hexV;
        }

        catch(Exception e)
        {
            System.out.println("Error: "+e.getMessage());
            return null;
        }
    }

    public static String _sign(final String ownerPrivateKey, final String encodedTransaction) {
        try
        {
            //Get key
            byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
            ECKey ownerKey = ECKey.fromPrivate(ownerPrivateKeyBytes);

            //Parse from hex encoded transaction
            byte[] transactionBytes = ByteArray.fromHexString(encodedTransaction);
            Protocol.Transaction transaction = Protocol.Transaction.parseFrom(transactionBytes);
            if(transaction == null)
            {
                //Problem creating transaction, reject and return
//                throw new Exception("Failed to _sign transaction");
                return encodedTransaction;
            }

            //Set timestamp and sign transaction
            transaction = TransactionUtils.setTimestamp(transaction);
            transaction = TransactionUtils.sign(transaction, ownerKey);

            //Get hex encoded string of signed transaction
            byte[] signedTransactionBytes = transaction.toByteArray();
            String encodedSignedTransaction = ByteArray.toHexString(signedTransactionBytes).toUpperCase();

            return encodedSignedTransaction;
        }
        catch(Exception e)
        {
            System.out.println("Error: "+e.getMessage());
            return "Error: "+e.getMessage();
        }
    }

    public static Protocol.Transaction setExpirationTime(Protocol.Transaction transaction, String timestamp) {
        if (transaction.getSignatureCount() == 0) {
            long expirationTime =  Long.parseLong(timestamp);
            Protocol.Transaction.Builder builder = transaction.toBuilder();
            org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
                    .toBuilder();
            rowBuilder.setExpiration(expirationTime);
            builder.setRawData(rowBuilder.build());
            return builder.build();
        }
        return transaction;
    }

    public static Protocol.Transaction setTimestamp(Protocol.Transaction transaction, String timestamp) {
        long currentTime = Long.parseLong(timestamp);
        Protocol.Transaction.Builder builder = transaction.toBuilder();
        org.tron.protos.Protocol.Transaction.raw.Builder rowBuilder = transaction.getRawData()
                .toBuilder();
        rowBuilder.setTimestamp(currentTime);
        builder.setRawData(rowBuilder.build());
        return builder.build();
    }

    public static Protocol.Transaction _sign(final String ownerPrivateKey, String timestamp, final Protocol.Transaction _transaction) {
        Protocol.Transaction transaction = null;
        //Get key
        byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
        ECKey ownerKey = ECKey.fromPrivate(ownerPrivateKeyBytes);

        if(_transaction == null)
        {
            //Problem creating transaction, reject and return
            return _transaction;
        }

        //Set timestamp and sign transaction
        transaction = setTimestamp(_transaction, timestamp);
        transaction = TransactionUtils.sign(transaction, ownerKey);
        return transaction;
    }

    public boolean isHexadecimal(String input) {
        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(input);
        return matcher.matches();
    }


    public static String[] getStringArray(JSONArray jsonArray) {
        String[] stringArray = null;
        if (jsonArray != null) {
            int length = jsonArray.toArray().length;
            stringArray = new String[length];
            for (int i = 0; i < length; i++) {
                System.out.println(" >>> "+ jsonArray.getString(i));
                stringArray[i] = jsonArray.getString(i);
            }
        }
        return stringArray;
    }

    public static Protocol.Transaction packTransaction(String payload) throws JsonFormat.ParseException {
        return Utils.packTransaction(payload, false);
    }

    public static JSONObject buildTriggerSmartContract(String payload) {


        JSONObject contractObject = JSONObject.parseObject(payload);

        String contractAddress = contractObject.getString("contractAddress");
        String functionSelector = contractObject.getString("functionSelector");

        String callValueString = contractObject.getJSONObject("options").getString("callValue");
//        String from = contractObject.getJSONObject("options").getString("from");
//        String shouldPollResponse = contractObject.getJSONObject("options").getString("shouldPollResponse");
        String tokenValueString = contractObject.getJSONObject("options").getString("tokenValue");
        String tokenIdString = contractObject.getJSONObject("options").getString("tokenId");
        String issuerAddress = contractObject.getString("issuerAddress");
        JSONArray parameters = contractObject.getJSONArray("parameters");

        Long callValue = Long.parseLong(callValueString);

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();

        byte[] contractAddressBytes = ByteArray.fromHexString(contractAddress);
        byte[] issuerAddressBytes = ByteArray.fromHexString(issuerAddress);

        String paramInput = "";
        if(parameters.toArray().length > 0) {
            paramInput = parseContractParamsString(parameters);
        }

        if (tokenIdString != null && tokenValueString != null) {
            Long tokenValue = Long.parseLong(tokenValueString);
            Long tokenId = Long.parseLong(tokenIdString);
            byte[] input = Hex.decode(AbiUtil.parseMethod(functionSelector, paramInput, false));

            builder.setOwnerAddress(ByteString.copyFrom(issuerAddressBytes));
            builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
            builder.setData(ByteString.copyFrom(input));
            builder.setCallValue(callValue);
            builder.setTokenId(tokenId);
            builder.setCallTokenValue(tokenValue);

        } else {
            byte[] input = Hex.decode(AbiUtil.parseMethod(functionSelector, paramInput, true));

            builder.setOwnerAddress(ByteString.copyFrom(issuerAddressBytes));
            builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
            builder.setData(ByteString.copyFrom(input));
            builder.setCallValue(callValue);
        }

        Contract.TriggerSmartContract triggerContract = builder.build();

        GrpcClient client = new GrpcClient(FULL_NODE_URL, SOLIDITY_NODE_URL);

        GrpcAPI.TransactionExtention transactionExtention = client.triggerContract(triggerContract);


        Protocol.Transaction unsignedTransaction = transactionExtention.getTransaction();

        return Utils.printTransactionToJSON(unsignedTransaction, false);
    }

    public static JSONObject validAddress(String input) {
        byte[] address = null;
        boolean result = true;
        String msg;
        try {
            if (input.length() == Constant.ADDRESS_SIZE) {
                //hex
                address = ByteArray.fromHexString(input);
                msg = "Hex";
            } else if (input.length() == 34) {
                //base58check
                address = TronWallet._decode58Check(input);
                msg = "Base58check";
            } else if (input.length() == 28) {
                //base64
                address = Base64.decode(input);
                msg = "Base64";
            } else {
                result = false;
                msg = "Length error";
            }
            if (result) {
                result = TronWallet.addressValid(address);
                if (!result) {
                    msg = "Invalid address";
                }
            }
        } catch (Exception e) {
            result = false;
            msg = e.getMessage();
        }

        JSONObject jsonAddress = new JSONObject();
        jsonAddress.put("result", result);
        jsonAddress.put("message", msg);

        return jsonAddress;
    }


    public static String parseContractParamsString(JSONArray parameters) {
        List<String> parameterList = new ArrayList<>();
        for(int i = 0; i < parameters.toArray().length; i++){
            JSONObject param = parameters.getJSONObject(i);

            String _key = parameters.getJSONObject(i).getString("type");
            String _value = parameters.getJSONObject(i).getString("value");

            // if address need to check format
            if (_key.equals("address")) {
                JSONObject resultAddressValidation = validAddress(_value);
                String _message = resultAddressValidation.getString("message");
                if (_message.equals("Length error")) {
                    return "Length error";
                }

                if (_message == "Invalid address") {
                    if (_value.startsWith("0x")) {
                        // convert to defaul hex value
                        _value = _value.replace("0x", Constant.ADD_PRE_FIX_STRING_MAINNET);
                        if (_value.length() == Constant.ADDRESS_SIZE) {
                            parameterList.add(_value);
                        }
                    } else {
                        return "Invalid address";
                    }
                }

                if (_message == "Hex") {
                    parameterList.add(_value);
                }

                if (_message == "Base64") {
                    byte[] parsedValueBytes = Base64.decode(_value);
                    String parsedValueString = ByteArray.toHexString(parsedValueBytes);
                    parameterList.add(parsedValueString);
                }

                if (_message == "Base58check") {
                    byte[] parsedValueBytes = TronWallet._decode58Check(_value);
                    String parsedValueString = ByteArray.toHexString(parsedValueBytes);
                    parameterList.add(parsedValueString);
                }
            }
        }

        StringBuilder paramStringBuilder = new StringBuilder();
        if (parameterList.toArray().length > 1) {
            for(String param : parameterList){
                paramStringBuilder.append(param);
                paramStringBuilder.append(",");
            }

            String paramInput = paramStringBuilder.toString();

            //Remove last comma
            paramInput = paramInput.substring(0, paramInput.length() - 1);
            return paramInput;
        } else {
            String paramInput = paramStringBuilder.toString();
            return paramInput;
        }
    }
}
