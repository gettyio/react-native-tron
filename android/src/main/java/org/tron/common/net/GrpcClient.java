package org.tron.common.net;


import com.google.protobuf.ByteString;
import org.tron.common.utils.ByteArray;

import org.apache.commons.lang3.StringUtils;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountPaginated;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockLimit;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.api.WalletExtensionGrpc;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Contract;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;

import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GrpcClient {

    private String TagName = "IGrpcClient";
    private ManagedChannel channelFull = null;
    private ManagedChannel channelSolidity = null;
    private WalletGrpc.WalletBlockingStub blockingStubFull = null;
    private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
    private WalletExtensionGrpc.WalletExtensionBlockingStub blockingStubExtension = null;
    private String[] fullNodeArray;
    private String[] solidityNodeArray;
    private int fCount, SCount;//连接fullNode节点 和连接solidityNode节点的次数
    private String currentFullnode;
    private String currentSol;


    public GrpcClient() {
        fCount = 3;
        SCount = 3;

//        initArray();
    }
//
//    private void initArray() {
//        if (SpAPI.THIS.isTest()) {
//            fullNodeArray = AppContextUtil.getContext().getResources().getStringArray(com.tron.tron_base.R.array.fullnodedemo_list);
//            solidityNodeArray = AppContextUtil.getContext().getResources().getStringArray(com.tron.tron_base.R.array.soliditynodedemo_list);
//        } else {
//            if (SpAPI.THIS.getFullNodes().size() != 0) {
//                fullNodeArray = new String[SpAPI.THIS.getFullNodes().size()];
//                SpAPI.THIS.getFullNodes().toArray(fullNodeArray);
//            } else {
//                fullNodeArray = AppContextUtil.getContext().getResources().getStringArray(R.array.fullnode_list);
//
//            }
//            if (SpAPI.THIS.getSolNodes().size() != 0) {
//                solidityNodeArray = new String[SpAPI.THIS.getSolNodes().size()];
//                SpAPI.THIS.getSolNodes().toArray(solidityNodeArray);
//            } else {
//                solidityNodeArray = AppContextUtil.getContext().getResources().getStringArray(R.array.soliditynode_list);
//            }
//        }
//
//    }

    public void connetFullNode(String fullnode) {
        try {
            if (!StringUtils.isEmpty(fullnode)) {
                channelFull = ManagedChannelBuilder.forTarget(fullnode)
                        .usePlaintext(true)
                        .build();
                blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
            } else {
                connetFullNode(randomFullNode());
            }
        } catch (Exception e) {
            if (fCount-- > 0) {
                connetFullNode(randomSolodityNode());
            }
        }
    }

    public void connetFullNode() {
        connetFullNode(randomFullNode());
    }

    public String randomFullNode() {
//        initArray();
        if (fullNodeArray == null || fullNodeArray.length < 1) return "";
        int index = (int) (Math.random() * fullNodeArray.length);
        currentFullnode = fullNodeArray[index];
        return currentFullnode;
    }

    public String randomSolodityNode() {
//        initArray();
        if (solidityNodeArray == null || solidityNodeArray.length < 1) return "";
        int index = (int) (Math.random() * solidityNodeArray.length);
        currentSol = solidityNodeArray[index];
        return currentSol;
    }

    public void connetSolidityNode(String soliditynode) {
        try {
            if (!StringUtils.isEmpty(soliditynode)) {
                channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
                        .usePlaintext(true)
                        .build();
                blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
                blockingStubExtension = WalletExtensionGrpc.newBlockingStub(channelSolidity);
            } else {
                connetSolidityNode(randomSolodityNode());
            }
        } catch (Exception e) {
            if (SCount-- > 0) {
                connetSolidityNode(randomSolodityNode());
            }
        }
    }

    public void connetSolidityNode() {
        connetSolidityNode(randomSolodityNode());
    }


    public void shutdown() throws InterruptedException {
        if (channelFull != null) {
            channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (channelSolidity != null) {
            channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public boolean canUseFullNode() {
        if (channelFull == null) return false;
        return !channelFull.isShutdown() && !channelFull.isTerminated();
    }

    public boolean canUseSolidityNode() {
        if (channelSolidity == null) return false;
        return !channelSolidity.isShutdown() && !channelSolidity.isTerminated();
    }

    @SuppressWarnings("unchecked")
    public Account queryAccount(byte[] address, boolean useSolidity) throws ConnectErrorException {
        try {
            ByteString addressBS = ByteString.copyFrom(address);
            Account request = Account.newBuilder().setAddress(addressBS).build();
            if (blockingStubSolidity != null && useSolidity) {
                return blockingStubSolidity.getAccount(request);
            } else {
                return blockingStubFull.getAccount(request);
            }
        } catch (Exception e) {
            if (!useSolidity) {
                connetFullNode(randomFullNode());
                throw new ConnectErrorException("FullNode-queryAccount  error ");
            } else {
                connetSolidityNode(randomSolodityNode());
                throw new ConnectErrorException("SolodityNode-queryAccount  error ");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public Account queryAccountById(String accountId) {
        ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
        Account request = Account.newBuilder().setAccountId(bsAccountId).build();
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.getAccountById(request);
        } else {
            return blockingStubFull.getAccountById(request);
        }
    }

    //Warning: do not invoke this interface provided by others.
    @SuppressWarnings("unchecked")
    public Transaction signTransaction(Protocol.TransactionSign transactionSign) {
        return blockingStubFull.getTransactionSign(transactionSign);
    }

    //Warning: do not invoke this interface provided by others.
    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention signTransaction2(Protocol.TransactionSign transactionSign) {
        return blockingStubFull.getTransactionSign2(transactionSign);
    }

    //Warning: do not invoke this interface provided by others.
    @SuppressWarnings("unchecked")
    public byte[] createAdresss(byte[] passPhrase) {
        BytesMessage.Builder builder = BytesMessage.newBuilder();
        builder.setValue(ByteString.copyFrom(passPhrase));

        BytesMessage result = blockingStubFull.createAddress(builder.build());
        return result.getValue().toByteArray();
    }

    //Warning: do not invoke this interface provided by others.
    @SuppressWarnings("unchecked")
    public GrpcAPI.EasyTransferResponse easyTransfer(byte[] passPhrase, byte[] toAddress, long amount) {
        GrpcAPI.EasyTransferMessage.Builder builder = GrpcAPI.EasyTransferMessage.newBuilder();
        builder.setPassPhrase(ByteString.copyFrom(passPhrase));
        builder.setToAddress(ByteString.copyFrom(toAddress));
        builder.setAmount(amount);

        return blockingStubFull.easyTransfer(builder.build());
    }

    //Warning: do not invoke this interface provided by others.
    @SuppressWarnings("unchecked")
    public GrpcAPI.EasyTransferResponse easyTransferByPrivate(byte[] privateKey, byte[] toAddress,
                                                              long amount) {
        GrpcAPI.EasyTransferByPrivateMessage.Builder builder = GrpcAPI.EasyTransferByPrivateMessage.newBuilder();
        builder.setPrivateKey(ByteString.copyFrom(privateKey));
        builder.setToAddress(ByteString.copyFrom(toAddress));
        builder.setAmount(amount);

        return blockingStubFull.easyTransferByPrivate(builder.build());
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.AccountUpdateContract contract) {
        return blockingStubFull.updateAccount(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.AccountUpdateContract contract) {
        return blockingStubFull.updateAccount2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.SetAccountIdContract contract) {
        return blockingStubFull.setAccountId(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.UpdateAssetContract contract) {
        return blockingStubFull.updateAsset(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.UpdateAssetContract contract) {
        return blockingStubFull.updateAsset2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.TransferContract contract) {
        return blockingStubFull.createTransaction(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.TransferContract contract) {
        return blockingStubFull.createTransaction2(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction(Contract.FreezeBalanceContract contract) {
        return blockingStubFull.freezeBalance2(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction(Contract.BuyStorageContract contract) {
        return blockingStubFull.buyStorage(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction(Contract.BuyStorageBytesContract contract) {
        return blockingStubFull.buyStorageBytes(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction(Contract.SellStorageContract contract) {
        return blockingStubFull.sellStorage(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.FreezeBalanceContract contract) {
        return blockingStubFull.freezeBalance2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.WithdrawBalanceContract contract) {
        return blockingStubFull.withdrawBalance(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.WithdrawBalanceContract contract) {
        return blockingStubFull.withdrawBalance2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.UnfreezeBalanceContract contract) {
        return blockingStubFull.unfreezeBalance(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.UnfreezeBalanceContract contract) {
        return blockingStubFull.unfreezeBalance2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransaction(Contract.UnfreezeAssetContract contract) {
        return blockingStubFull.unfreezeAsset(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransaction2(Contract.UnfreezeAssetContract contract) {
        return blockingStubFull.unfreezeAsset2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createTransferAssetTransaction(Contract.TransferAssetContract contract) {
        return blockingStubFull.transferAsset(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createTransferAssetTransaction2(
            Contract.TransferAssetContract contract) {
        return blockingStubFull.transferAsset2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createParticipateAssetIssueTransaction(
            Contract.ParticipateAssetIssueContract contract) {
        return blockingStubFull.participateAssetIssue(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createParticipateAssetIssueTransaction2(
            Contract.ParticipateAssetIssueContract contract) {
        return blockingStubFull.participateAssetIssue2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createAssetIssue(Contract.AssetIssueContract contract) {
        return blockingStubFull.createAssetIssue(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createAssetIssue2(Contract.AssetIssueContract contract) {
        return blockingStubFull.createAssetIssue2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction voteWitnessAccount(Contract.VoteWitnessContract contract) {
        return blockingStubFull.voteWitnessAccount(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention voteWitnessAccount2(Contract.VoteWitnessContract contract) {
        return blockingStubFull.voteWitnessAccount2(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention proposalCreate(Contract.ProposalCreateContract contract) {
        return blockingStubFull.proposalCreate(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.ProposalList listProposals() {
        GrpcAPI.ProposalList proposalList = blockingStubFull.listProposals(EmptyMessage.newBuilder().build());
        return proposalList;
    }

    @SuppressWarnings("unchecked")
    public Protocol.Proposal getProposal(String id) {
        BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
                ByteArray.fromLong(Long.parseLong(id))))
                .build();
        Protocol.Proposal proposal = blockingStubFull.getProposalById(request);
        return proposal;
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.ExchangeList listExchanges() {
        GrpcAPI.ExchangeList exchangeList = blockingStubFull.listExchanges(EmptyMessage.newBuilder().build());
        return exchangeList;
    }

    @SuppressWarnings("unchecked")
    public Protocol.Exchange getExchange(String id) {
        BytesMessage request = BytesMessage.newBuilder().setValue(ByteString.copyFrom(
                ByteArray.fromLong(Long.parseLong(id))))
                .build();
        Protocol.Exchange exchange = blockingStubFull.getExchangeById(request);
        return exchange;
    }

    @SuppressWarnings("unchecked")
    public Protocol.ChainParameters getChainParameters() {
        Protocol.ChainParameters chainParameters = blockingStubFull
                .getChainParameters(EmptyMessage.newBuilder().build());
        return chainParameters;
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention proposalApprove(Contract.ProposalApproveContract contract) {
        return blockingStubFull.proposalApprove(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention proposalDelete(Contract.ProposalDeleteContract contract) {
        return blockingStubFull.proposalDelete(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention exchangeCreate(Contract.ExchangeCreateContract contract) {
        return blockingStubFull.exchangeCreate(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention exchangeInject(Contract.ExchangeInjectContract contract) {
        return blockingStubFull.exchangeInject(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention exchangeWithdraw(Contract.ExchangeWithdrawContract contract) {
        return blockingStubFull.exchangeWithdraw(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention exchangeTransaction(Contract.ExchangeTransactionContract contract) {
        return blockingStubFull.exchangeTransaction(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction createAccount(Contract.AccountCreateContract contract) {
        return blockingStubFull.createAccount(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createAccount2(Contract.AccountCreateContract contract) {
        return blockingStubFull.createAccount2(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.AddressPrKeyPairMessage generateAddress(EmptyMessage emptyMessage) {
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.generateAddress(emptyMessage);
        } else {
            return blockingStubFull.generateAddress(emptyMessage);
        }
    }

    @SuppressWarnings("unchecked")
    public Transaction createWitness(Contract.WitnessCreateContract contract) {
        return blockingStubFull.createWitness(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention createWitness2(Contract.WitnessCreateContract contract) {
        return blockingStubFull.createWitness2(contract);
    }

    @SuppressWarnings("unchecked")
    public Transaction updateWitness(Contract.WitnessUpdateContract contract) {
        return blockingStubFull.updateWitness(contract);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.TransactionExtention updateWitness2(Contract.WitnessUpdateContract contract) {
        return blockingStubFull.updateWitness2(contract);
    }

    @SuppressWarnings("unchecked")
    public boolean broadcastTransaction(Transaction signaturedTransaction) {
        int i = 10;
        GrpcAPI.Return response = blockingStubFull.broadcastTransaction(signaturedTransaction);
        while (response.getResult() == false && response.getCode() == response_code.SERVER_BUSY
                && i > 0) {
            i--;
            response = blockingStubFull.broadcastTransaction(signaturedTransaction);
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (response.getResult() == false) {
        }
        return response.getResult();
    }

    @SuppressWarnings("unchecked")
    public Block getBlock(long blockNum) {
        if (blockNum < 0) {
            if (blockingStubSolidity != null) {
                return blockingStubSolidity.getNowBlock(EmptyMessage.newBuilder().build());
            } else {
                return blockingStubFull.getNowBlock(EmptyMessage.newBuilder().build());
            }
        }
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.getBlockByNum(builder.build());
        } else {
            return blockingStubFull.getBlockByNum(builder.build());
        }
    }

    @SuppressWarnings("unchecked")
    public long getTransactionCountByBlockNum(long blockNum) {
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.getTransactionCountByBlockNum(builder.build()).getNum();
        } else {
            return blockingStubFull.getTransactionCountByBlockNum(builder.build()).getNum();
        }
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.BlockExtention getBlock2(long blockNum) {
        if (blockNum < 0) {
            if (blockingStubSolidity != null) {
                return blockingStubSolidity.getNowBlock2(EmptyMessage.newBuilder().build());
            } else {
                return blockingStubFull.getNowBlock2(EmptyMessage.newBuilder().build());
            }
        }
        NumberMessage.Builder builder = NumberMessage.newBuilder();
        builder.setNum(blockNum);
        if (blockingStubSolidity != null) {
            return blockingStubSolidity.getBlockByNum2(builder.build());
        } else {
            return blockingStubFull.getBlockByNum2(builder.build());
        }
    }

    //  public Optional<AccountList> listAccounts() {
//    AccountList accountList = blockingStubSolidity
//        .listAccounts(EmptyMessage.newBuilder().build());
//    return Optional.ofNullable(accountList);
//
//  }

    public WitnessList listWitnesses() {
//        if (blockingStubSolidity != null) {
//            WitnessList witnessList = blockingStubSolidity
//                    .listWitnesses(EmptyMessage.newBuilder().build());
//            return witnessList;
//        } else {
        WitnessList witnessList = blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
        return witnessList;
//        }
    }

    public AssetIssueList getAssetIssueList() {
        if (blockingStubSolidity != null) {
            AssetIssueList assetIssueList = blockingStubSolidity
                    .getAssetIssueList(EmptyMessage.newBuilder().build());
            return assetIssueList;
        } else {
            AssetIssueList assetIssueList = blockingStubFull
                    .getAssetIssueList(EmptyMessage.newBuilder().build());
            return assetIssueList;
        }
    }

    @SuppressWarnings("unchecked")
    public AssetIssueList getAssetIssueList(long offset, long limit) {
        GrpcAPI.PaginatedMessage.Builder pageMessageBuilder = GrpcAPI.PaginatedMessage.newBuilder();
        pageMessageBuilder.setOffset(offset);
        pageMessageBuilder.setLimit(limit);
        if (blockingStubSolidity != null) {
            AssetIssueList assetIssueList = blockingStubSolidity.
                    getPaginatedAssetIssueList(pageMessageBuilder.build());
            return assetIssueList;
        } else {
            AssetIssueList assetIssueList = blockingStubFull
                    .getPaginatedAssetIssueList(pageMessageBuilder.build());
            return assetIssueList;
        }
    }



    @SuppressWarnings("unchecked")
    public AssetIssueList getAssetIssueByAccount(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        AssetIssueList assetIssueList = blockingStubFull.getAssetIssueByAccount(request);
        return assetIssueList;
    }

    @SuppressWarnings("unchecked")
    public AccountNetMessage getAccountNet(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccountNet(request);
    }

    @SuppressWarnings("unchecked")
    public GrpcAPI.AccountResourceMessage getAccountRes(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccountResource(request);
    }

    public GrpcAPI.AccountResourceMessage getAccountResource(byte[] address) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account request = Account.newBuilder().setAddress(addressBS).build();
        return blockingStubFull.getAccountResource(request);
    }

    @SuppressWarnings("unchecked")
    public Contract.AssetIssueContract getAssetIssueByName(String assetName) {
        ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
        BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
        return blockingStubFull.getAssetIssueByName(request);
    }

    @SuppressWarnings("unchecked")
    public NumberMessage getTotalTransaction() {
        return blockingStubFull.totalTransaction(EmptyMessage.newBuilder().build());
    }

    public NumberMessage getNextMaintenanceTime() {
        return blockingStubFull.getNextMaintenanceTime(EmptyMessage.newBuilder().build());
    }

//  public Optional<AssetIssueList> getAssetIssueListByTimestamp(long time) {
//    NumberMessage.Builder timeStamp = NumberMessage.newBuilder();
//    timeStamp.setNum(time);
//    AssetIssueList assetIssueList = blockingStubSolidity
//        .getAssetIssueListByTimestamp(timeStamp.build());
//    return Optional.ofNullable(assetIssueList);
//  }

//  public Optional<TransactionList> getTransactionsByTimestamp(long start, long end, int offset,
//      int limit) {
//    TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
//    timeMessage.setBeginInMilliseconds(start);
//    timeMessage.setEndInMilliseconds(end);
//    TimePaginatedMessage.Builder timePaginatedMessage = TimePaginatedMessage.newBuilder();
//    timePaginatedMessage.setTimeMessage(timeMessage);
//    timePaginatedMessage.setOffset(offset);
//    timePaginatedMessage.setLimit(limit);
//    TransactionList transactionList = blockingStubExtension
//        .getTransactionsByTimestamp(timePaginatedMessage.build());
//    return Optional.ofNullable(transactionList);
//  }

    //  public NumberMessage getTransactionsByTimestampCount(long start, long end) {
//    TimeMessage.Builder timeMessage = TimeMessage.newBuilder();
//    timeMessage.setBeginInMilliseconds(start);
//    timeMessage.setEndInMilliseconds(end);
//    return blockingStubExtension.getTransactionsByTimestampCount(timeMessage.build());
//  }
    public TransactionList getTransactionsFromThis(byte[] address, int offset, int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        TransactionList transactionList = blockingStubExtension
                .getTransactionsFromThis(accountPaginated.build());
        return transactionList;
    }

    public GrpcAPI.TransactionListExtention getTransactionsFromThis2(byte[] address, int offset,
                                                                     int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
                .getTransactionsFromThis2(accountPaginated.build());
        return transactionList;
    }

    //  public NumberMessage getTransactionsFromThisCount(byte[] address) {
//    ByteString addressBS = ByteString.copyFrom(address);
//    Account account = Account.newBuilder().setAddress(addressBS).build();
//    return blockingStubExtension.getTransactionsFromThisCount(account);
//  }
    public TransactionList getTransactionsToThis(byte[] address, int offset, int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        TransactionList transactionList = blockingStubExtension
                .getTransactionsToThis(accountPaginated.build());
        return transactionList;
    }

    public GrpcAPI.TransactionListExtention getTransactionsToThis2(byte[] address, int offset,
                                                                   int limit) {
        ByteString addressBS = ByteString.copyFrom(address);
        Account account = Account.newBuilder().setAddress(addressBS).build();
        AccountPaginated.Builder accountPaginated = AccountPaginated.newBuilder();
        accountPaginated.setAccount(account);
        accountPaginated.setOffset(offset);
        accountPaginated.setLimit(limit);
        GrpcAPI.TransactionListExtention transactionList = blockingStubExtension
                .getTransactionsToThis2(accountPaginated.build());
        return transactionList;
    }

    //  public NumberMessage getTransactionsToThisCount(byte[] address) {
//    ByteString addressBS = ByteString.copyFrom(address);
//    Account account = Account.newBuilder().setAddress(addressBS).build();
//    return blockingStubExtension.getTransactionsToThisCount(account);
//  }
    public Transaction getTransactionById(String txID) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Transaction transaction = blockingStubFull.getTransactionById(request);
        return transaction;
    }

    public Protocol.TransactionInfo getTransactionInfoById(String txID) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Protocol.TransactionInfo transactionInfo;
        if (blockingStubSolidity != null) {
            transactionInfo = blockingStubSolidity.getTransactionInfoById(request);
        } else {
            transactionInfo = blockingStubFull.getTransactionInfoById(request);
        }
        return transactionInfo;
    }

    public Block getBlockById(String blockID) {
        ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(blockID));
        BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
        Block block = blockingStubFull.getBlockById(request);
        return block;
    }

    public BlockList getBlockByLimitNext(long start, long end) {
        BlockLimit.Builder builder = BlockLimit.newBuilder();
        builder.setStartNum(start);
        builder.setEndNum(end);
        BlockList blockList = blockingStubFull.getBlockByLimitNext(builder.build());
        return blockList;
    }

    public GrpcAPI.BlockListExtention getBlockByLimitNext2(long start, long end) {
        BlockLimit.Builder builder = BlockLimit.newBuilder();
        builder.setStartNum(start);
        builder.setEndNum(end);
        GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLimitNext2(builder.build());
        return blockList;
    }

    public BlockList getBlockByLatestNum(long num) {
        NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
        BlockList blockList = blockingStubFull.getBlockByLatestNum(numberMessage);
        return blockList;
    }

    public GrpcAPI.BlockListExtention getBlockByLatestNum2(long num) {
        NumberMessage numberMessage = NumberMessage.newBuilder().setNum(num).build();
        GrpcAPI.BlockListExtention blockList = blockingStubFull.getBlockByLatestNum2(numberMessage);
        return blockList;
    }

    public GrpcAPI.TransactionExtention updateSetting(Contract.UpdateSettingContract request) {
        return blockingStubFull.updateSetting(request);
    }

    public GrpcAPI.TransactionExtention deployContract(Contract.CreateSmartContract request) {
        return blockingStubFull.deployContract(request);
    }

    public GrpcAPI.TransactionExtention triggerContract(Contract.TriggerSmartContract request) {
        return blockingStubFull.triggerContract(request);
    }

    public Protocol.SmartContract getContract(byte[] address) {
        ByteString byteString = ByteString.copyFrom(address);
        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
        return blockingStubFull.getContract(bytesMessage);
    }

    public GrpcAPI.DelegatedResourceList getDelegatedResource(byte[] from, byte[] to) {
        ByteString fromHex = ByteString.copyFrom(from);
        ByteString toHex = ByteString.copyFrom(to);
        GrpcAPI.DelegatedResourceMessage bytesMessage = GrpcAPI.DelegatedResourceMessage.newBuilder().setFromAddress(fromHex).setToAddress(toHex).build();
        return blockingStubFull.getDelegatedResource(bytesMessage);
    }


    public Protocol.DelegatedResourceAccountIndex getDelegatedResourceAccountIndex(byte[] from) {
        ByteString byteString = ByteString.copyFrom(from);
        BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
        return blockingStubFull.getDelegatedResourceAccountIndex(bytesMessage);
    }
}
