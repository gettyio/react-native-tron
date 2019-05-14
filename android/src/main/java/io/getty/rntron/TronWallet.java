package io.getty.rntron;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcClient;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonFormat;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.Utils;
import org.tron.core.Constant;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.math.BigInteger;
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

    private static String addressPreFixString = Constant.ADD_PRE_FIX_STRING_MAINNET;  //default testnet
    private static byte addressPreFixByte = Constant.ADD_PRE_FIX_BYTE_MAINNET;
    public static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

    public static byte[] decodeFromBase58Check(String addressBase58) {
        if (StringUtils.isEmpty(addressBase58)) {
            System.out.println("Warning: Address is empty !!");
            return null;
        }
        byte[] address = _decode58Check(addressBase58);
        if (address == null) {
            return null;
        }

        if (!addressValid(address)) {
            return null;
        }

        return address;
    }

    public static byte[] _decode58Check(String input)
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

    public static String _encode58Check(byte[] input)
    {
        byte[] hash0 = Sha256Hash.hash(input);
        byte[] hash1 = Sha256Hash.hash(hash0);
        byte[] inputCheck = new byte[input.length + 4];
        System.arraycopy(input, 0, inputCheck, 0, input.length);
        System.arraycopy(hash1, 0, inputCheck, input.length, 4);
        return Base58.encode(inputCheck);
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


    public static String signString(final String ownerPrivateKey, final String hexString) {
        try
        {
            String newHex = hexString.replace("0x", Constant.ADD_PRE_FIX_STRING_MAINNET);

            //Get key
            byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
            byte[] transactionBytes = ByteArray.fromHexString(newHex);
            byte[] hash = Sha256Hash.hash(transactionBytes);

            ECKey ownerKey = ECKey.fromPrivate(ownerPrivateKeyBytes);

            ECKey.ECDSASignature signature = ownerKey.sign(hash);
            ByteString bsSign = ByteString.copyFrom(signature.toByteArray());

            return "0x"+ByteArray.toHexString(bsSign.toByteArray());
        }

        catch(Exception e)
        {
            System.out.println("Error: "+e.getMessage());
            return "Error: "+e.getMessage();
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

    public static SignatureData signMessage(byte[] message, ECKeyPair keyPair, boolean needToHash) {
        BigInteger publicKey = keyPair.getPublicKey();
        byte[] messageHash;
        if (needToHash) {
            messageHash = Hash.sha3(message);
        } else {
            messageHash = message;
        }

        ECKey.ECDSASignature sig = keyPair.sign(messageHash);
        // Now we have to work backwards to figure out the recId needed to recover the signature.
        int recId = -1;
        for (int i = 0; i < 4; i++) {
            BigInteger k = ECKey.recoverFromSignature(i, sig, messageHash);
            if (k != null && k.equals(publicKey)) {
                recId = i;
                break;
            }
        }
        if (recId == -1) {
            throw new RuntimeException(
                    "Could not construct a recoverable key. Are your credentials valid?");
        }

        int headerByte = recId + 27;

        // 1 header + 32 bytes for R + 32 bytes for S
        byte v = (byte) headerByte;
        byte[] r = Numeric.toBytesPadded(sig.r, 32);
        byte[] s = Numeric.toBytesPadded(sig.s, 32);

        return new SignatureData(v, r, s);
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

        System.out.println("HEY IM HERE!");
        System.out.println(Utils.printTransactionToJSON(transaction, false));
        return transaction;
    }
//    //        bfdeaf18e8d44515a4749a5ebdfeede9d67a396a9284ea582eac287952a4ce67
//    public static JSONObject buildTriggerSmartContract2(String ownerPrivateKey, String stringTransaction) throws JsonFormat.ParseException {
//        Protocol.Transaction tr = Utils.packTransaction(stringTransaction, false);
//
//        byte[] rawDataBytes = ByteArray.fromHexString("0a022c372208f91fb13c67bc4cc040b0e59f9daa2d5ab301081f12ae010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412790a1541756bf73fc5d7042aaa4a4b8488befc777a2581351215412ec5f63da00583085d4c2c5e8ec3c8d17bde5e281880ade2042244a3082be90000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000170bea09c9daa2d9001809bee02");
//        ByteString rawDataByteString = ByteString.copyFrom(rawDataBytes);
//
//        Contract.TriggerSmartContract.Builder build = Contract.TriggerSmartContract.newBuilder();
//        GrpcAPI.TransactionExtention.Builder trxExtBuilder = GrpcAPI.TransactionExtention.newBuilder();
//        GrpcAPI.Return.Builder retBuilder = GrpcAPI.Return.newBuilder();
//        boolean visible = false;
//
//        try {
//            String contract = stringTransaction;
//            JsonFormat.merge(contract, build, visible);
//
//            JSONObject jsonObject = JSONObject.parseObject(contract);
//            String selector = jsonObject.getString("function_selector");
//            String parameter = jsonObject.getString("parameter");
//            String data = Utils.parseMethod(selector, parameter);
//            build.setData(ByteString.copyFrom(ByteArray.fromHexString(data)));
//            build.setCallTokenValue(jsonObject.getLongValue("call_token_value"));
//            build.setTokenId(jsonObject.getLongValue("token_id"));
//            build.setCallValue(jsonObject.getLongValue("call_value"));
//            long feeLimit = jsonObject.getLongValue("fee_limit");
//
//
//            GrpcAPI.TransactionExtention.Builder txBuilder = GrpcAPI.TransactionExtention.newBuilder();
//            Protocol.Transaction.Builder rawBuilder = Protocol.Transaction.newBuilder();
//
//
//            rawBuilder.setFeeLimit(feeLimit);
//            txBuilder.setRawData(rawBuilder);
//
//            Protocol.Transaction newTx2 = txBuilder.build();
//
//            Protocol.Transaction newTx = Protocol.Transaction.newBuilder()
//                    .setRawData(
//                            Protocol
//                                    .Transaction
//                                    .raw
//                                    .newBuilder()
//                                    .setTimestamp(1557523796030L)
//                                    .setRefBlockBytes(ByteString.copyFromUtf8("2c37"))
//                                    .setRefBlockHash(ByteString.copyFromUtf8("f91fb13c67bc4cc0"))
//                                    .setExpiration(1557523854000L)
//                                    .setFeeLimit(6000000L)
//                                    .addContract(
//                                            Protocol.Transaction.Contract.newBuilder()
//                                                    .setType(Protocol.Transaction.Contract.ContractType.TriggerSmartContract)
//                                                    .setParameter(Any.pack(newTx2))
//                                                    .build())
//                                    .build()
//                    )
//                    .build();
//
//            tr = TronWallet._sign(ownerPrivateKey, tr);
//
//
//        } catch (ContractValidateException e) {
//            retBuilder.setResult(false).setCode(response_code.CONTRACT_VALIDATE_ERROR)
//                    .setMessage(ByteString.copyFromUtf8(e.getMessage()));
//        } catch (Exception e) {
//            retBuilder.setResult(false).setCode(response_code.OTHER_ERROR)
//                    .setMessage(ByteString.copyFromUtf8(e.getClass() + " : " + e.getMessage()));
//        }
//
//
//        return Utils.printTransactionToJSON(tr, true);
//    }

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
            System.out.println("paramInput: "+paramInput);
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

//
//        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
//            System.out.println("RPC create call trx failed!");
//            System.out.println("Code = " + transactionExtention.getResult().getCode());
//            System.out
//                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
//            return null;
//        }
//        Protocol.Transaction transaction = transactionExtention.getTransaction();
//        if (transaction.getRetCount() != 0
//                && transactionExtention.getConstantResult(0) != null
//                && transactionExtention.getResult() != null) {
//            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
//            System.out.println("message:" + transaction.getRet(0).getRet());
//            System.out.println(":" + ByteArray
//                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
//            System.out.println("Result:" + Hex.toHexString(result));
//            return null;
//        }
//
//        final GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
//        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
//        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
//                .toBuilder();
//        rawBuilder.setFeeLimit(feeLimit);
//        transBuilder.setRawData(rawBuilder);
//        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
//            ByteString s = transactionExtention.getTransaction().getSignature(i);
//            transBuilder.setSignature(i, s);
//        }
//        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
//            Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
//            transBuilder.setRet(i, r);
//        }
//        texBuilder.setTransaction(transBuilder);
//        texBuilder.setResult(transactionExtention.getResult());
//        texBuilder.setTxid(transactionExtention.getTxid());
//        transactionExtention = texBuilder.build();
//        if (transactionExtention == null) {
//            return null;
//        }
//        GrpcAPI.Return ret = transactionExtention.getResult();
//        if (!ret.getResult()) {
//            System.out.println("Code = " + ret.getCode());
//            System.out.println("Message = " + ret.getMessage().toStringUtf8());
//            return null;
//        }
//        transaction = transactionExtention.getTransaction();
//        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
//            System.out.println("Transaction is empty");
//            return null;
//        }
//        System.out.println(
//                "trigger txid = " + ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData()
//                        .toByteArray())));
////        transaction = signTransaction(transaction, blockingStubFull, permissionKeyString);
////
////        broadcastTransaction(transaction, blockingStubFull);
//        return transaction;

//        JSONObject contractObject = JSONObject.parseObject(payload);
//
//        String contractAddress = contractObject.getString("contractAddress");
//        String functionSelector = contractObject.getString("functionSelector");
//
//        String feeLimitString = contractObject.getJSONObject("options").getString("feeLimit");
//        String callValueString = contractObject.getJSONObject("options").getString("callValue");
//        String issuerAddress = contractObject.getString("issuerAddress");
//
//        Long feeLimit = Long.parseLong(feeLimitString);
//        Long callValue = Long.parseLong(callValueString);
//
//        byte[] contractAddressBytes = ByteArray.fromHexString(contractAddress);
//        byte[] issuerAddressBytes = ByteArray.fromHexString(issuerAddress);
//
//
//        JSONArray parameters = contractObject.getJSONArray("parameters");
//
//        String paramInput = getInputString(parameters);
//
//        byte[] input = Hex.decode(AbiUtil.parseMethod(functionSelector, paramInput, true));
//
//        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
//
//        builder.setOwnerAddress(ByteString.copyFrom(issuerAddressBytes));
//        builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
//        builder.setData(ByteString.copyFrom(input));
//        builder.setCallValue(callValue);
//
//        Contract.TriggerSmartContract contract = builder.build();
//
//        GrpcClient client = new GrpcClient(FULL_NODE_URL, SOLIDITY_NODE_URL);
//
//        GrpcAPI.TransactionExtention transactionExtention;
////    if (isConstant) {
////      transactionExtention = client.triggerConstantContract(contract);
////    } else {
////      transactionExtention = client.triggerContract(contract);
////    }
//        transactionExtention = client.triggerContract(contract);
//
//        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
//            System.out.println("RPC create call trx failed!");
//            System.out.println("Code = " + transactionExtention.getResult().getCode());
//            System.out.println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
//
//            Map map = new HashMap();
//            map.put("status", "false");
//            return new JSONObject(map);
//        }
//
//        Protocol.Transaction transaction = transactionExtention.getTransaction();
//        // for constant
//        if (transaction.getRetCount() != 0 &&
//                transactionExtention.getConstantResult(0) != null &&
//                transactionExtention.getResult() != null) {
//            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
//            System.out.println("message:" + transaction.getRet(0).getRet());
//            System.out.println(":" + ByteArray
//                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
//            System.out.println("Result:" + Hex.toHexString(result));
//            Map map = new HashMap();
//            map.put("status", "true");
//            return new JSONObject(map);
////            return returnMessage;
//        }
//
//        GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
//        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
//        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
//        rawBuilder.setFeeLimit(6000000);
//        transBuilder.setRawData(rawBuilder);
//
//        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
//            ByteString s = transactionExtention.getTransaction().getSignature(i);
//            transBuilder.setSignature(i, s);
//        }
//
//        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
//            Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
//            transBuilder.setRet(i, r);
//        }
//
//        texBuilder.setTransaction(transBuilder);
//        texBuilder.setResult(transactionExtention.getResult());
//        texBuilder.setTxid(transactionExtention.getTxid());
//        transactionExtention = texBuilder.build();
//
//        if (transactionExtention == null) {
//            Map map = new HashMap();
//            map.put("status", "false");
//            return new JSONObject(map);
//        }
//
//        GrpcAPI.Return ret = transactionExtention.getResult();
//
//        if (!ret.getResult()) {
//            System.out.println("Code = " + ret.getCode());
//            System.out.println("Message = " + ret.getMessage().toStringUtf8());
//            Map map = new HashMap();
//            map.put("status", "false");
//            return new JSONObject(map);
//        }
//
//        transaction = transactionExtention.getTransaction();
//
//        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
//            System.out.println("Transaction is empty");
//            Map map = new HashMap();
//            map.put("status", "false");
//            return new JSONObject(map);
//        }
////        System.out.println(
////                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
////        transaction = TronWallet._sign(ownerPrivateKey, transaction);
////        byte[] signedTransactionBytes = transaction.toByteArray();
////        String encodedSignedTransaction = ByteArray.toHexString(signedTransactionBytes).toUpperCase();
////
////        String[] signatures = new String[transaction.getSignatureCount()];
////        for (int i = 0; i < transaction.getSignatureCount(); i++) {
////            signatures[i] = TransactionUtils.getHexFromByteString(transaction.getSignature(i));
////            System.out.println(TransactionUtils.getHexFromByteString(transaction.getSignature(i)));
////        }
//
//        return Utils.printTransactionToJSON(transaction, false);
// String ownerPrivateKey, String contractAddress, String callValueStr, String methodStr, String argsStr, String tokenValueStr, String tokenId

//        byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
//        ECKey key = ECKey.fromPrivate(ownerPrivateKeyBytes);
//{"contractAddress":"41b2b9efe94cc9f548c2ee1755b217606db7521dcd","functionSelector":"getStakeInfoByAddress(address)","options":{"feeLimit":1000000,"callValue":0}
// ,"parameters":[{"type":"address","value":"4183d4d7a6260a6689afb8d1b0b87be10b71c8e26a"}],"callback":false}
//        //Get public address
//        byte[] addressBytes = key.getAddress();
//        byte[] contractAddressBytes = TronWallet._decode58Check(contractAddress);
//        byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, false));
//
//        Long callValue = Long.parseLong(callValueStr);
//        Long tokenValue = Long.parseLong(tokenValueStr);
//
//        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
////
//        builder.setOwnerAddress(ByteString.copyFrom(addressBytes));
//        builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
//        builder.setData(ByteString.copyFrom(input));
//        builder.setCallValue(callValue);

//        if (tokenId != null && tokenId != "") {
//            builder.setCallTokenValue(tokenValue);
//            builder.setTokenId(Long.parseLong(tokenId));
//        }
//
//        Contract.TriggerSmartContract contract = builder.build();
//
//        System.out.println(contract);
//
//        GrpcClient client = new GrpcClient(FULL_NODE_URL, SOLIDITY_NODE_URL);
//
//        GrpcAPI.TransactionExtention transactionExtention;
////    if (isConstant) {
////      transactionExtention = client.triggerConstantContract(contract);
////    } else {
////      transactionExtention = client.triggerContract(contract);
////    }
//        transactionExtention = client.triggerContract(contract);
//
//        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
//            System.out.println("RPC create call trx failed!");
//            System.out.println("Code = " + transactionExtention.getResult().getCode());
//            System.out
//                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
//            returnMessage = "false";
////            return returnMessage;
//        }
//
//        Protocol.Transaction transaction = transactionExtention.getTransaction();
//        // for constant
//        if (transaction.getRetCount() != 0 &&
//                transactionExtention.getConstantResult(0) != null &&
//                transactionExtention.getResult() != null) {
//            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
//            System.out.println("message:" + transaction.getRet(0).getRet());
//            System.out.println(":" + ByteArray
//                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
//            System.out.println("Result:" + Hex.toHexString(result));
//            returnMessage = "true";
////            return returnMessage;
//        }
//
//        GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
//        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
//        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
//        rawBuilder.setFeeLimit(6000000);
//        transBuilder.setRawData(rawBuilder);
//
//        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
//            ByteString s = transactionExtention.getTransaction().getSignature(i);
//            transBuilder.setSignature(i, s);
//        }
//
//        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
//            Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
//            transBuilder.setRet(i, r);
//        }
//
//        texBuilder.setTransaction(transBuilder);
//        texBuilder.setResult(transactionExtention.getResult());
//        texBuilder.setTxid(transactionExtention.getTxid());
//        transactionExtention = texBuilder.build();
//
//        if (transactionExtention == null) {
//            returnMessage = "false";
////            return returnMessage;
//        }
//
//        GrpcAPI.Return ret = transactionExtention.getResult();
//
//        if (!ret.getResult()) {
//            System.out.println("Code = " + ret.getCode());
//            System.out.println("Message = " + ret.getMessage().toStringUtf8());
//            returnMessage = "false";
////            return returnMessage;
//        }
//
//        transaction = transactionExtention.getTransaction();
//
//        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
//            System.out.println("Transaction is empty");
//            returnMessage = "false";
////            return returnMessage;
//        }
//        System.out.println(
//                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
//        transaction = TronWallet._sign(ownerPrivateKey, transaction);
//        byte[] signedTransactionBytes = transaction.toByteArray();
//        String encodedSignedTransaction = ByteArray.toHexString(signedTransactionBytes).toUpperCase();
//
//        String[] signatures = new String[transaction.getSignatureCount()];
//        for (int i = 0; i < transaction.getSignatureCount(); i++) {
//            signatures[i] = TransactionUtils.getHexFromByteString(transaction.getSignature(i));
//            System.out.println(TransactionUtils.getHexFromByteString(transaction.getSignature(i)));
//        }
//
//        return Utils.printTransactionToJSON(transaction, false);
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
                    byte[] parsedValueBytes = TronWallet.decodeFromBase58Check(_value);
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
