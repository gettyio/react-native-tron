package org.tron.common.net;


import com.google.protobuf.ByteString;
import org.tron.common.utils.StringUtils;
import org.tron.common.crypto.Hash;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.TransactionUtils;
import org.tron.common.utils.abi.AbiUtil;
import org.tron.common.utils.abi.CancelException;
import org.tron.common.utils.abi.EncodingException;

import org.spongycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TronAPI {


    public static Protocol.Account queryAccount(byte[] address, boolean useSolidity) throws ConnectErrorException {
        return IGrpcClient.THIS.getCli().queryAccount(address, useSolidity);
    }

    //TODO 更改为createTransferAssetTransaction2
    public static GrpcAPI.TransactionExtention createTransferAssetTransaction(byte[] to, byte[] assertName, byte[] owner, long amount) {
        Contract.TransferAssetContract contract = createTransferAssetContract(to, assertName, owner,
                amount);
        return IGrpcClient.THIS.getCli().createTransferAssetTransaction2(contract);
    }

    public static Protocol.Transaction createParticipateAssetIssueTransaction(byte[] to, byte[] assertName,
                                                                              byte[] owner, long amount) {
        Contract.ParticipateAssetIssueContract contract = createParticipateAssetIssueContract(to, assertName,
                owner, amount);
        return IGrpcClient.THIS.getCli().createParticipateAssetIssueTransaction(contract);
    }

    public static Protocol.Transaction createUpdateAccountTransaction(byte[] addressBytes, String accountName) {
        Contract.AccountUpdateContract contract = createAccountUpdateContract(ByteArray.fromString(accountName),
                addressBytes);
        return IGrpcClient.THIS.getCli().createTransaction(contract);
    }

    public static boolean broadcastTransaction(Protocol.Transaction transaction) {
        return TransactionUtils.validTransaction(transaction)
                && IGrpcClient.THIS.getCli().broadcastTransaction(transaction);
    }

    public static Protocol.Transaction createWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessCreateContract contract = createWitnessCreateContract(owner, url);
        return IGrpcClient.THIS.getCli().createWitness(contract);
    }

    public static Protocol.Transaction createUpdateWitnessTransaction(byte[] owner, byte[] url) {
        Contract.WitnessUpdateContract contract = createWitnessUpdateContract(owner, url);
        return IGrpcClient.THIS.getCli().updateWitness(contract);
    }

    public static Protocol.Transaction createVoteWitnessTransaction(byte[] owner, HashMap<String, String> witness) {
        Contract.VoteWitnessContract contract = createVoteWitnessContract(owner, witness);
        return IGrpcClient.THIS.getCli().voteWitnessAccount(contract);
    }


    public static Protocol.Transaction createAssetIssueTransaction(Contract.AssetIssueContract contract) {
        return IGrpcClient.THIS.getCli().createAssetIssue(contract);
    }

    public static Protocol.Block getBlock(long blockNum) {
        return IGrpcClient.THIS.getCli().getBlock(blockNum);
    }


    public static Protocol.Transaction createTransaction4Transfer(Contract.TransferContract contract) {
        return IGrpcClient.THIS.getCli().createTransaction(contract);
    }

    public static GrpcAPI.AssetIssueList getAssetIssueList() {
        return IGrpcClient.THIS.getCli().getAssetIssueList();
    }

    public static GrpcAPI.AssetIssueList getAssetIssueByAccount(byte[] address) {
        return IGrpcClient.THIS.getCli().getAssetIssueByAccount(address);
    }

    public static GrpcAPI.AccountNetMessage getAccountNet(byte[] address) {
        return IGrpcClient.THIS.getCli().getAccountNet(address);
    }

    public static GrpcAPI.AccountResourceMessage getAccountRes(byte[] address) {
        return IGrpcClient.THIS.getCli().getAccountRes(address);
    }

    public static Contract.AssetIssueContract getAssetIssueByName(String assetName) {
        return IGrpcClient.THIS.getCli().getAssetIssueByName(assetName);
    }

    public static GrpcAPI.NumberMessage getTotalTransaction() {
        return IGrpcClient.THIS.getCli().getTotalTransaction();
    }

    public static GrpcAPI.NumberMessage getNextMaintenanceTime() {
        return IGrpcClient.THIS.getCli().getNextMaintenanceTime();
    }

    public static GrpcAPI.TransactionList getTransactionsFromThis(byte[] address, int offset, int limit) {
        return IGrpcClient.THIS.getCli().getTransactionsFromThis(address, offset, limit);
    }

    public static GrpcAPI.TransactionList getTransactionsToThis(byte[] address, int offset, int limit) {
        return IGrpcClient.THIS.getCli().getTransactionsToThis(address, offset, limit);
    }

    public static Protocol.Transaction getTransactionById(String txID) {
        return IGrpcClient.THIS.getCli().getTransactionById(txID);
    }


    public static GrpcAPI.TransactionExtention createFreezeBalanceTransaction(byte[] owner, long frozen_balance, long frozen_duration, byte[] receiver_address, Contract.ResourceCode resource) {
        Contract.FreezeBalanceContract contract = createFreezeBalanceContract(owner, frozen_balance, frozen_duration, receiver_address, resource);
        return IGrpcClient.THIS.getCli().createTransaction(contract);
    }

    public static GrpcAPI.TransactionExtention createUnfreezeBalanceTransaction(byte[] owner, Contract.ResourceCode resourceCode) {
        Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(owner, resourceCode);
        return IGrpcClient.THIS.getCli().createTransaction2(contract);
    }


    public static GrpcAPI.TransactionExtention createUnfreezeBalanceTransaction(byte[] owner, byte[] receiver, Contract.ResourceCode resourceCode) {
        Contract.UnfreezeBalanceContract contract = createUnfreezeBalanceContract(owner, receiver, resourceCode);
        return IGrpcClient.THIS.getCli().createTransaction2(contract);
    }


    public static Protocol.Block getBlockById(String blockID) {
        return IGrpcClient.THIS.getCli().getBlockById(blockID);
    }

    public static GrpcAPI.BlockList getBlockByLimitNext(long start, long end) {
        return IGrpcClient.THIS.getCli().getBlockByLimitNext(start, end);
    }

    public static GrpcAPI.BlockList getBlockByLatestNum(long num) {
        return IGrpcClient.THIS.getCli().getBlockByLatestNum(num);
    }

    public static Protocol.SmartContract getContract(byte[] address) {
        return IGrpcClient.THIS.getCli().getContract(address);
    }

    public static GrpcAPI.DelegatedResourceList getDelegatedResource(byte[] from, byte[] to) {
        return IGrpcClient.THIS.getCli().getDelegatedResource(from, to);
    }

    public static Protocol.DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(byte[] from) {
        return IGrpcClient.THIS.getCli().getDelegatedResourceAccountIndex(from);
    }

    public static GrpcAPI.TransactionExtention triggerContract(byte[] owner, byte[] contractAddress, long callValue, byte[] data, long feeLimit)
            throws IOException, CipherException, CancelException {
        Contract.TriggerSmartContract triggerContract = triggerCallContract(owner, contractAddress,
                callValue, data);
        GrpcAPI.TransactionExtention transactionExtention = IGrpcClient.THIS.getCli().triggerContract(triggerContract);
        if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
//            System.out.println("RPC create call trx failed!");
//            System.out.println("Code = " + transactionExtention.getResult().getCode());
//            System.out
//                    .println("Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
            return null;
        }

        Protocol.Transaction transaction = transactionExtention.getTransaction();
        if (transaction.getRetCount() != 0 &&
                transactionExtention.getConstantResult(0) != null &&
                transactionExtention.getResult() != null) {
            byte[] result = transactionExtention.getConstantResult(0).toByteArray();

            return transactionExtention;
        }

        GrpcAPI.TransactionExtention.Builder texBuilder = GrpcAPI.TransactionExtention.newBuilder();
        Protocol.Transaction.Builder transBuilder = Protocol.Transaction.newBuilder();
        Protocol.Transaction.raw.Builder rawBuilder = transactionExtention.getTransaction().getRawData()
                .toBuilder();
        rawBuilder.setFeeLimit(feeLimit);
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

        return transactionExtention;
    }

    public static GrpcAPI.TransactionExtention triggerContract(String[] parameters, byte[] ower)
            throws IOException, CipherException, CancelException, EncodingException {
        if (parameters == null ||
                parameters.length < 6) {
            System.out.println("TriggerContract needs 6 parameters like following: ");
            System.out.println(
                    "TriggerContract contractAddress method args isHex fee_limit value");
            return null;
        }

        String contractAddrStr = parameters[0];
        String methodStr = parameters[1];
        String argsStr = parameters[2];
        boolean isHex = Boolean.valueOf(parameters[3]);
        long feeLimit = Long.valueOf(parameters[4]);
        long callValue = Long.valueOf(parameters[5]);
        if (argsStr.equalsIgnoreCase("#")) {
            argsStr = "";
        }
        byte[] input = Hex.decode(AbiUtil.parseMethod(methodStr, argsStr, isHex));
        byte[] contractAddress = StringUtils.decodeFromBase58Check(contractAddrStr);

        return triggerContract(ower, contractAddress, callValue, input, feeLimit);
    }

    public static Contract.TriggerSmartContract triggerCallContract(byte[] address,
                                                                    byte[] contractAddress,
                                                                    long callValue, byte[] data) {
        Contract.TriggerSmartContract.Builder builder = Contract.TriggerSmartContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(address));
        builder.setContractAddress(ByteString.copyFrom(contractAddress));
        builder.setData(ByteString.copyFrom(data));
        builder.setCallValue(callValue);
        return builder.build();
    }


    public static GrpcAPI.WitnessList listWitnesses() {
        GrpcAPI.WitnessList witnessList = IGrpcClient.THIS.getCli().listWitnesses();
        if (witnessList != null) {
            List<Protocol.Witness> list = witnessList.getWitnessesList();
            List<Protocol.Witness> newList = new ArrayList();
            newList.addAll(list);
            GrpcAPI.WitnessList.Builder builder = GrpcAPI.WitnessList.newBuilder();
            for (Protocol.Witness witness : newList) {
                builder.addWitnesses(witness);
            }
            witnessList = builder.build();
        }
        return witnessList;
    }

    public static Contract.AssetIssueContract createAssetIssueContract(byte[] owner, String name, String abbr, long totalSupply, int trxNum, int icoNum,
                                                                       long startTime, long endTime, int voteScore, String description, String url,
                                                                       long freeNetLimit, long publicFreeNetLimit, List<Contract.AssetIssueContract.FrozenSupply> frozenSupply) {
        Contract.AssetIssueContract.Builder builder = Contract.AssetIssueContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));

        builder.setName(ByteString.copyFrom(name.getBytes()));
        builder.setAbbr(ByteString.copyFrom(abbr.getBytes()));

        if (totalSupply <= 0) {
            return null;
        }
        builder.setTotalSupply(totalSupply);
        if (trxNum <= 0) {
            return null;
        }
        builder.setTrxNum(trxNum);
        if (icoNum <= 0) {
            return null;
        }
        builder.setNum(icoNum);
        long now = System.currentTimeMillis();
        if (startTime <= now) {
            return null;
        }
        if (endTime <= startTime) {
            return null;
        }
        if (freeNetLimit < 0) {
            return null;
        }
        if (publicFreeNetLimit < 0) {
            return null;
        }

        builder.setStartTime(startTime);
        builder.setEndTime(endTime);
        builder.setVoteScore(voteScore);
        builder.setDescription(ByteString.copyFrom(description.getBytes()));
        builder.setUrl(ByteString.copyFrom(url.getBytes()));
        builder.setFreeAssetNetLimit(freeNetLimit);
        builder.setPublicFreeAssetNetLimit(publicFreeNetLimit);

        builder.addAllFrozenSupply(frozenSupply);

        return builder.build();
    }

    public static Contract.FreezeBalanceContract createFreezeBalanceContract(byte[] owner, long frozen_balance,
                                                                             long frozen_duration, byte[] receiver_address, Contract.ResourceCode resource) {
        Contract.FreezeBalanceContract.Builder builder = Contract.FreezeBalanceContract.newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);


        builder.setOwnerAddress(byteAddreess).setFrozenBalance(frozen_balance)
                .setFrozenDuration(frozen_duration)
                .setResource(resource);
        if (receiver_address != null && receiver_address.length != 0) {
            ByteString byteReceiverAddress = ByteString.copyFrom(receiver_address);
            builder.setReceiverAddress(byteReceiverAddress);
        }
        return builder.build();
    }


    public static Contract.AccountUpdateContract createAccountUpdateContract(byte[] accountName,
                                                                             byte[] address) {
        Contract.AccountUpdateContract.Builder builder = Contract.AccountUpdateContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        ByteString bsAccountName = ByteString.copyFrom(accountName);
        builder.setAccountName(bsAccountName);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.UpdateAssetContract createUpdateAssetContract(
            byte[] address,
            byte[] description,
            byte[] url,
            long newLimit,
            long newPublicLimit
    ) {
        Contract.UpdateAssetContract.Builder builder =
                Contract.UpdateAssetContract.newBuilder();
        ByteString basAddreess = ByteString.copyFrom(address);
        builder.setDescription(ByteString.copyFrom(description));
        builder.setUrl(ByteString.copyFrom(url));
        builder.setNewLimit(newLimit);
        builder.setNewPublicLimit(newPublicLimit);
        builder.setOwnerAddress(basAddreess);

        return builder.build();
    }

    public static Contract.WitnessCreateContract createWitnessCreateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessCreateContract.Builder builder = Contract.WitnessCreateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.WitnessUpdateContract createWitnessUpdateContract(byte[] owner,
                                                                             byte[] url) {
        Contract.WitnessUpdateContract.Builder builder = Contract.WitnessUpdateContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        builder.setUpdateUrl(ByteString.copyFrom(url));

        return builder.build();
    }

    public static Contract.VoteWitnessContract createVoteWitnessContract(byte[] owner,
                                                                         HashMap<String, String> witness) {
        Contract.VoteWitnessContract.Builder builder = Contract.VoteWitnessContract.newBuilder();
        builder.setOwnerAddress(ByteString.copyFrom(owner));
        for (String addressBase58 : witness.keySet()) {
            String value = witness.get(addressBase58);
            long count = Long.parseLong(value);
            Contract.VoteWitnessContract.Vote.Builder voteBuilder = Contract.VoteWitnessContract.Vote
                    .newBuilder();
            byte[] address = StringUtils.decodeFromBase58Check(addressBase58);
            if (address == null) {
                continue;
            }
            voteBuilder.setVoteAddress(ByteString.copyFrom(address));
            voteBuilder.setVoteCount(count);
            builder.addVotes(voteBuilder.build());
        }

        return builder.build();
    }


    public static Contract.UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] owner, Contract.ResourceCode resourceCode) {

        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);

        builder.setOwnerAddress(byteAddreess);
        builder.setResource(resourceCode);

        return builder.build();
    }

    public static Contract.UnfreezeBalanceContract createUnfreezeBalanceContract(byte[] owner, byte[] receiver, Contract.ResourceCode resourceCode) {

        Contract.UnfreezeBalanceContract.Builder builder = Contract.UnfreezeBalanceContract
                .newBuilder();
        ByteString byteAddreess = ByteString.copyFrom(owner);

        ByteString receiverByteAddreess = ByteString.copyFrom(receiver);


        builder.setOwnerAddress(byteAddreess);
        builder.setReceiverAddress(receiverByteAddreess);
        builder.setResource(resourceCode);

        return builder.build();
    }


    public static boolean isTransactionConfirmed(Protocol.Transaction transaction) {
        Protocol.Transaction confirmedTransaction = null;
        String txID = Hex.toHexString(Hash.sha256(transaction.getRawData().toByteArray()));

        int maxTries = 5;
        int tries = 0;
        while ((confirmedTransaction == null || !confirmedTransaction.hasRawData()) && tries <= maxTries) {
            try {
                confirmedTransaction = getTransactionById(txID);
                tries++;
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        return confirmedTransaction.hasRawData() && txID.equals(Hex.toHexString(Hash.sha256(confirmedTransaction.getRawData().toByteArray())));
    }

    public static Contract.TransferContract createTransferContract(byte[] to, byte[] owner,
                                                                   long amount) {
        Contract.TransferContract.Builder builder = Contract.TransferContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.TransferAssetContract createTransferAssetContract(byte[] to,
                                                                             byte[] assertName, byte[] owner,
                                                                             long amount) {
        Contract.TransferAssetContract.Builder builder = Contract.TransferAssetContract.newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

    public static Contract.ParticipateAssetIssueContract createParticipateAssetIssueContract(byte[] to,
                                                                                             byte[] assertName, byte[] owner,
                                                                                             long amount) {
        Contract.ParticipateAssetIssueContract.Builder builder = Contract.ParticipateAssetIssueContract
                .newBuilder();
        ByteString bsTo = ByteString.copyFrom(to);
        ByteString bsName = ByteString.copyFrom(assertName);
        ByteString bsOwner = ByteString.copyFrom(owner);
        builder.setToAddress(bsTo);
        builder.setAssetName(bsName);
        builder.setOwnerAddress(bsOwner);
        builder.setAmount(amount);

        return builder.build();
    }

}
