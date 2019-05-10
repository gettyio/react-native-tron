package io.getty.rntron;

import com.google.protobuf.ByteString;

import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcClient;
import org.tron.common.crypto.ECKey;
import org.tron.common.crypto.Sha256Hash;
import org.tron.common.utils.AbiUtil;
import org.tron.common.utils.Base58;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.common.utils.Utils;

import java.security.SecureRandom;
import java.util.Arrays;

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

    public static Protocol.Transaction _sign(final String ownerPrivateKey, final Protocol.Transaction _transaction) {
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
        transaction = TransactionUtils.setTimestamp(_transaction);
        transaction = TransactionUtils.sign(transaction, ownerKey);
        return transaction;
    }

    public static String buildTriggerSmartContract(String ownerPrivateKey, String contractAddress, String callValueStr, String methodStr, String argsStr, String tokenValueStr, String tokenId) {
        String returnMessage;

        byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(ownerPrivateKey);
        ECKey key = ECKey.fromPrivate(ownerPrivateKeyBytes);

        //Get public address
        byte[] addressBytes = key.getAddress();
        byte[] contractAddressBytes = TronWallet._decode58Check(contractAddress);
        byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, false));

        Long callValue = Long.parseLong(callValueStr);
        Long tokenValue = Long.parseLong(tokenValueStr);

        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();

        builder.setOwnerAddress(ByteString.copyFrom(addressBytes));
        builder.setContractAddress(ByteString.copyFrom(contractAddressBytes));
        builder.setData(ByteString.copyFrom(input));
        builder.setCallValue(callValue);

        if (tokenId != null && tokenId != "") {
            builder.setCallTokenValue(tokenValue);
            builder.setTokenId(Long.parseLong(tokenId));
        }

        Contract.TriggerSmartContract contract = builder.build();

        System.out.println(contract);

        GrpcClient client = new GrpcClient(FULL_NODE_URL, SOLIDITY_NODE_URL);

        GrpcAPI.TransactionExtention transactionExtention;
//    if (isConstant) {
//      transactionExtention = client.triggerConstantContract(contract);
//    } else {
//      transactionExtention = client.triggerContract(contract);
//    }
        transactionExtention = client.triggerContract(contract);

        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
            System.out.println("RPC create call trx failed!");
            System.out.println("Code = " + transactionExtention.getResult().getCode());
            System.out
                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            returnMessage = "false";
            return returnMessage;
        }

        Protocol.Transaction transaction = transactionExtention.getTransaction();
        // for constant
        if (transaction.getRetCount() != 0 &&
                transactionExtention.getConstantResult(0) != null &&
                transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();
            System.out.println("message:" + transaction.getRet(0).getRet());
            System.out.println(":" + ByteArray
                    .toStr(transactionExtention.getResult().getMessage().toByteArray()));
            System.out.println("Result:" + Hex.toHexString(result));
            returnMessage = "true";
            return returnMessage;
        }

        GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData().toBuilder();
        rawBuilder.setFeeLimit(6000000);
        transBuilder.setRawData(rawBuilder);

        for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
            ByteString s = transactionExtention.getTransaction().getSignature(i);
            transBuilder.setSignature(i, s);
        }

        for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
            Protocol.Transaction.Result r = transactionExtention.getTransaction().getRet(i);
            transBuilder.setRet(i, r);
        }

        texBuilder.setTransaction(transBuilder);
        texBuilder.setResult(transactionExtention.getResult());
        texBuilder.setTxid(transactionExtention.getTxid());
        transactionExtention = texBuilder.build();

        if (transactionExtention == null) {
            returnMessage = "false";
            return returnMessage;
        }

        GrpcAPI.Return ret = transactionExtention.getResult();

        if (!ret.getResult()) {
            System.out.println("Code = " + ret.getCode());
            System.out.println("Message = " + ret.getMessage().toStringUtf8());
            returnMessage = "false";
            return returnMessage;
        }

        transaction = transactionExtention.getTransaction();

        if (transaction == null || transaction.getRawData().getContractCount() == 0) {
            System.out.println("Transaction is empty");
            returnMessage = "false";
            return returnMessage;
        }
        System.out.println(
                "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
        transaction = TronWallet._sign(ownerPrivateKey, transaction);
        byte[] signedTransactionBytes = transaction.toByteArray();
        String encodedSignedTransaction = ByteArray.toHexString(signedTransactionBytes).toUpperCase();

        String[] signatures = new String[transaction.getSignatureCount()];
        for (int i = 0; i < transaction.getSignatureCount(); i++) {
            signatures[i] = TransactionUtils.getHexFromByteString(transaction.getSignature(i));
            System.out.println(TransactionUtils.getHexFromByteString(transaction.getSignature(i)));
        }

        return Utils.printTransaction(transaction);
    }
}
