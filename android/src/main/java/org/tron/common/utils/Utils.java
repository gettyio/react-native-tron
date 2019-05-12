/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BlockList;
import org.tron.api.GrpcAPI.BlockListExtention;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.ProposalList;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionList;
import org.tron.api.GrpcAPI.TransactionListExtention;
import org.tron.api.GrpcAPI.TransactionSignWeight;
import org.tron.api.GrpcAPI.WitnessList;
import org.tron.common.crypto.Hash;
import org.tron.common.crypto.Sha256Hash;
import org.tron.protos.Contract.AccountCreateContract;
import org.tron.protos.Contract.AccountPermissionUpdateContract;
import org.tron.protos.Contract.AccountUpdateContract;
import org.tron.protos.Contract.AssetIssueContract;
import org.tron.protos.Contract.AssetIssueContract.FrozenSupply;
import org.tron.protos.Contract.CreateSmartContract;
import org.tron.protos.Contract.ExchangeCreateContract;
import org.tron.protos.Contract.ExchangeInjectContract;
import org.tron.protos.Contract.ExchangeTransactionContract;
import org.tron.protos.Contract.ExchangeWithdrawContract;
import org.tron.protos.Contract.FreezeBalanceContract;
import org.tron.protos.Contract.ParticipateAssetIssueContract;
import org.tron.protos.Contract.ProposalApproveContract;
import org.tron.protos.Contract.ProposalCreateContract;
import org.tron.protos.Contract.ProposalDeleteContract;
import org.tron.protos.Contract.SetAccountIdContract;
import org.tron.protos.Contract.TransferAssetContract;
import org.tron.protos.Contract.TransferContract;
import org.tron.protos.Contract.TriggerSmartContract;
import org.tron.protos.Contract.UnfreezeAssetContract;
import org.tron.protos.Contract.UnfreezeBalanceContract;
import org.tron.protos.Contract.UpdateAssetContract;
import org.tron.protos.Contract.UpdateEnergyLimitContract;
import org.tron.protos.Contract.UpdateSettingContract;
import org.tron.protos.Contract.VoteAssetContract;
import org.tron.protos.Contract.VoteWitnessContract;
import org.tron.protos.Contract.WithdrawBalanceContract;
import org.tron.protos.Contract.WitnessCreateContract;
import org.tron.protos.Contract.WitnessUpdateContract;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.BlockHeader;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.ChainParameters.ChainParameter;
import org.tron.protos.Protocol.DelegatedResource;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Proposal;
import org.tron.protos.Protocol.ResourceReceipt;
import org.tron.protos.Protocol.SmartContract;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.Witness;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.getty.rntron.TronWallet;

public class Utils {
  public static final String PERMISSION_ID = "Permission_id";
  public static final String VISIBLE = "visible";
  public static final String TRANSACTION = "transaction";
  public static final String VALUE = "value";

  private static SecureRandom random = new SecureRandom();

  public static SecureRandom getRandom() {
    return random;
  }

  public static String parseMethod(String methodSign, String input) {
    byte[] selector = new byte[4];
    System.arraycopy(Hash.sha3(methodSign.getBytes()), 0, selector, 0, 4);
    //System.out.println(methodSign + ":" + Hex.toHexString(selector));
    if (input.length() == 0) {
      return Hex.toHexString(selector);
    }

    return Hex.toHexString(selector) + input;
  }

  public static byte[] getBytes(char[] chars) {
    Charset cs = Charset.forName("UTF-8");
    CharBuffer cb = CharBuffer.allocate(chars.length);
    cb.put(chars);
    cb.flip();
    ByteBuffer bb = cs.encode(cb);

    return bb.array();
  }

  private char[] getChars(byte[] bytes) {
    Charset cs = Charset.forName("UTF-8");
    ByteBuffer bb = ByteBuffer.allocate(bytes.length);
    bb.put(bytes);
    bb.flip();
    CharBuffer cb = cs.decode(bb);

    return cb.array();
  }

  /**
   * yyyy-MM-dd
   */
  public static Date strToDateLong(String strDate) {
    if (strDate.length() == 10) {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    } else if (strDate.length() == 19) {
      strDate = strDate.replace("_", " ");
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      ParsePosition pos = new ParsePosition(0);
      Date strtodate = formatter.parse(strDate, pos);
      return strtodate;
    }
    return null;
  }


  public static String printWitness(Witness witness) {
    String result = "";
    result += "address: ";
    result += TronWallet._encode58Check(witness.getAddress().toByteArray());
    result += "\n";
    result += "voteCount: ";
    result += witness.getVoteCount();
    result += "\n";
    result += "pubKey: ";
    result += ByteArray.toHexString(witness.getPubKey().toByteArray());
    result += "\n";
    result += "url: ";
    result += witness.getUrl();
    result += "\n";
    result += "totalProduced: ";
    result += witness.getTotalProduced();
    result += "\n";
    result += "totalMissed: ";
    result += witness.getTotalMissed();
    result += "\n";
    result += "latestBlockNum: ";
    result += witness.getLatestBlockNum();
    result += "\n";
    result += "latestSlotNum: ";
    result += witness.getLatestSlotNum();
    result += "\n";
    result += "isJobs: ";
    result += witness.getIsJobs();
    result += "\n";
    return result;
  }

  public static String printProposal(Proposal proposal) {
    String result = "";
    result += "id: ";
    result += proposal.getProposalId();
    result += "\n";
    result += "state: ";
    result += proposal.getState();
    result += "\n";
    result += "createTime: ";
    result += proposal.getCreateTime();
    result += "\n";
    result += "expirationTime: ";
    result += proposal.getExpirationTime();
    result += "\n";
    result += "parametersMap: ";
    result += proposal.getParametersMap();
    result += "\n";
    result += "approvalsList: [ \n";
    for (ByteString address : proposal.getApprovalsList()) {
      result += TronWallet._encode58Check(address.toByteArray());
      result += "\n";
    }
    result += "]";
    return result;
  }

  public static String printProposalsList(ProposalList proposalList) {
    String result = "\n";
    int i = 0;
    for (Proposal proposal : proposalList.getProposalsList()) {
      result += "proposal " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printProposal(proposal);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printTransactionExtention(TransactionExtention transactionExtention,
                                                 boolean selfType) {
    String string = JsonFormat.printToString(transactionExtention, selfType);
    JSONObject jsonObject = JSONObject.parseObject(string);
    if (transactionExtention.getResult().getResult()) {
      JSONObject transactionOjbect = printTransactionToJSON(
              transactionExtention.getTransaction(), selfType);
      transactionOjbect.put(VISIBLE, selfType);
      jsonObject.put(TRANSACTION, transactionOjbect);
    }
    return jsonObject.toJSONString();
  }

  public static JSONObject printTransactionToJSON(Transaction transaction, boolean selfType) {
    JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction,
            selfType));
    JSONArray contracts = new JSONArray();
    transaction.getRawData().getContractList().stream().forEach(contract -> {
      try {
        JSONObject contractJson = null;
        Any contractParameter = contract.getParameter();
        switch (contract.getType()) {
          case AccountCreateContract:
            AccountCreateContract accountCreateContract = contractParameter
                    .unpack(AccountCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountCreateContract,
                    selfType));
            break;
          case TransferContract:
            TransferContract transferContract = contractParameter.unpack(TransferContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferContract,
                    selfType));
            break;
          case TransferAssetContract:
            TransferAssetContract transferAssetContract = contractParameter
                    .unpack(TransferAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(transferAssetContract,
                    selfType));
            break;
          case VoteAssetContract:
            VoteAssetContract voteAssetContract = contractParameter.unpack(VoteAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteAssetContract,
                    selfType));
            break;
          case VoteWitnessContract:
            VoteWitnessContract voteWitnessContract = contractParameter
                    .unpack(VoteWitnessContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(voteWitnessContract,
                    selfType));
            break;
          case WitnessCreateContract:
            WitnessCreateContract witnessCreateContract = contractParameter
                    .unpack(WitnessCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessCreateContract,
                    selfType));
            break;
          case AssetIssueContract:
            AssetIssueContract assetIssueContract = contractParameter
                    .unpack(AssetIssueContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(assetIssueContract,
                    selfType));
            break;
          case WitnessUpdateContract:
            WitnessUpdateContract witnessUpdateContract = contractParameter
                    .unpack(WitnessUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(witnessUpdateContract,
                    selfType));
            break;
          case ParticipateAssetIssueContract:
            ParticipateAssetIssueContract participateAssetIssueContract = contractParameter
                    .unpack(ParticipateAssetIssueContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(participateAssetIssueContract, selfType));
            break;
          case AccountUpdateContract:
            AccountUpdateContract accountUpdateContract = contractParameter
                    .unpack(AccountUpdateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(accountUpdateContract,
                    selfType));
            break;
          case FreezeBalanceContract:
            FreezeBalanceContract freezeBalanceContract = contractParameter
                    .unpack(FreezeBalanceContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(freezeBalanceContract,
                    selfType));
            break;
          case UnfreezeBalanceContract:
            UnfreezeBalanceContract unfreezeBalanceContract = contractParameter
                    .unpack(UnfreezeBalanceContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(unfreezeBalanceContract, selfType));
            break;
          case WithdrawBalanceContract:
            WithdrawBalanceContract withdrawBalanceContract = contractParameter
                    .unpack(WithdrawBalanceContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(withdrawBalanceContract, selfType));
            break;
          case UnfreezeAssetContract:
            UnfreezeAssetContract unfreezeAssetContract = contractParameter
                    .unpack(UnfreezeAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(unfreezeAssetContract,
                    selfType));
            break;
          case UpdateAssetContract:
            UpdateAssetContract updateAssetContract = contractParameter
                    .unpack(UpdateAssetContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateAssetContract,
                    selfType));
            break;
          case ProposalCreateContract:
            ProposalCreateContract proposalCreateContract = contractParameter
                    .unpack(ProposalCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalCreateContract,
                    selfType));
            break;
          case ProposalApproveContract:
            ProposalApproveContract proposalApproveContract = contractParameter
                    .unpack(ProposalApproveContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(proposalApproveContract, selfType));
            break;
          case ProposalDeleteContract:
            ProposalDeleteContract proposalDeleteContract = contractParameter
                    .unpack(ProposalDeleteContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(proposalDeleteContract,
                    selfType));
            break;
//          case SetAccountIdContract:
//            Contract.SetAccountIdContract setAccountIdContract =
//                    contractParameter.unpack(Contract.SetAccountIdContract.class);
//            contractJson = JSONObject.parseObject(JsonFormat.printToString(setAccountIdContract,
//                    selfType));
//            break;
//          case CreateSmartContract:
//            CreateSmartContract deployContract = contractParameter
//                    .unpack(CreateSmartContract.class);
//            contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract,
//                    selfType));
//            byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
//            byte[] contractAddress = generateContractAddress(transaction, ownerAddress);
//            jsonTransaction.put("contract_address", ByteArray.toHexString(contractAddress));
//            break;
          case TriggerSmartContract:
            TriggerSmartContract triggerSmartContract = contractParameter
                    .unpack(TriggerSmartContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(triggerSmartContract,
                    selfType));
            break;
          case UpdateSettingContract:
            UpdateSettingContract updateSettingContract = contractParameter
                    .unpack(UpdateSettingContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(updateSettingContract,
                    selfType));
            break;
          case ExchangeCreateContract:
            ExchangeCreateContract exchangeCreateContract = contractParameter
                    .unpack(ExchangeCreateContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeCreateContract,
                    selfType));
            break;
          case ExchangeInjectContract:
            ExchangeInjectContract exchangeInjectContract = contractParameter
                    .unpack(ExchangeInjectContract.class);
            contractJson = JSONObject.parseObject(JsonFormat.printToString(exchangeInjectContract,
                    selfType));
            break;
          case ExchangeWithdrawContract:
            ExchangeWithdrawContract exchangeWithdrawContract = contractParameter
                    .unpack(ExchangeWithdrawContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(exchangeWithdrawContract, selfType));
            break;
          case ExchangeTransactionContract:
            ExchangeTransactionContract exchangeTransactionContract = contractParameter
                    .unpack(ExchangeTransactionContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(exchangeTransactionContract, selfType));
            break;
          case UpdateEnergyLimitContract:
            UpdateEnergyLimitContract updateEnergyLimitContract = contractParameter
                    .unpack(UpdateEnergyLimitContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(updateEnergyLimitContract, selfType));
            break;
          case AccountPermissionUpdateContract:
            AccountPermissionUpdateContract accountPermissionUpdateContract = contractParameter
                    .unpack(AccountPermissionUpdateContract.class);
            contractJson = JSONObject
                    .parseObject(JsonFormat.printToString(accountPermissionUpdateContract, selfType));
            break;
//          case ClearABIContract:
//            Contract.ClearABIContract clearABIContract = contractParameter
//                    .unpack(Contract.ClearABIContract.class);
//            contractJson = JSONObject
//                    .parseObject(JsonFormat.printToString(clearABIContract, selfType));
//            break;
          // todo add other contract
          default:
        }
        JSONObject parameter = new JSONObject();
        parameter.put(VALUE, contractJson);
        parameter.put("type_url", contract.getParameterOrBuilder().getTypeUrl());
        JSONObject jsonContract = new JSONObject();
        jsonContract.put("parameter", parameter);
        jsonContract.put("type", contract.getType());
        if (contract.getPermissionId() > 0) {
          jsonContract.put(PERMISSION_ID, contract.getPermissionId());
        }
        contracts.add(jsonContract);
      } catch (InvalidProtocolBufferException e) {
        System.out.println(e.getMessage());
      }
    });

    JSONObject rawData = JSONObject.parseObject(jsonTransaction.get("raw_data").toString());
    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    String rawDataHex = ByteArray.toHexString(transaction.getRawData().toByteArray());
    jsonTransaction.put("raw_data_hex", rawDataHex);
    String txID = ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    jsonTransaction.put("txID", txID);
    return jsonTransaction;
  }

  public static Transaction packTransaction(String strTransaction, boolean selfType) throws JsonFormat.ParseException {
    JSONObject jsonTransaction = JSONObject.parseObject(strTransaction);
    JSONObject rawData = jsonTransaction.getJSONObject("raw_data");
    JSONArray contracts = new JSONArray();
    JSONArray rawContractArray = rawData.getJSONArray("contract");

    for (int i = 0; i < rawContractArray.size(); i++) {
      try {
        JSONObject contract = rawContractArray.getJSONObject(i);
        JSONObject parameter = contract.getJSONObject("parameter");
        String contractType = contract.getString("type");
        Any any = null;
        switch (contractType) {
          case "AccountCreateContract":
            AccountCreateContract.Builder accountCreateContractBuilder = AccountCreateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    accountCreateContractBuilder, selfType);
            any = Any.pack(accountCreateContractBuilder.build());
            break;
          case "TransferContract":
            TransferContract.Builder transferContractBuilder = TransferContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    transferContractBuilder, selfType);
            any = Any.pack(transferContractBuilder.build());
            break;
          case "TransferAssetContract":
            TransferAssetContract.Builder transferAssetContractBuilder = TransferAssetContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    transferAssetContractBuilder, selfType);
            any = Any.pack(transferAssetContractBuilder.build());
            break;
          case "VoteAssetContract":
            VoteAssetContract.Builder voteAssetContractBuilder = VoteAssetContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    voteAssetContractBuilder, selfType);
            any = Any.pack(voteAssetContractBuilder.build());
            break;
          case "VoteWitnessContract":
            VoteWitnessContract.Builder voteWitnessContractBuilder =
                    VoteWitnessContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    voteWitnessContractBuilder, selfType);
            any = Any.pack(voteWitnessContractBuilder.build());
            break;
          case "WitnessCreateContract":
            WitnessCreateContract.Builder witnessCreateContractBuilder = WitnessCreateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    witnessCreateContractBuilder, selfType);
            any = Any.pack(witnessCreateContractBuilder.build());
            break;
          case "AssetIssueContract":
            AssetIssueContract.Builder assetIssueContractBuilder = AssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    assetIssueContractBuilder, selfType);
            any = Any.pack(assetIssueContractBuilder.build());
            break;
          case "WitnessUpdateContract":
            WitnessUpdateContract.Builder witnessUpdateContractBuilder = WitnessUpdateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    witnessUpdateContractBuilder, selfType);
            any = Any.pack(witnessUpdateContractBuilder.build());
            break;
          case "ParticipateAssetIssueContract":
            ParticipateAssetIssueContract.Builder participateAssetIssueContractBuilder =
                    ParticipateAssetIssueContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    participateAssetIssueContractBuilder, selfType);
            any = Any.pack(participateAssetIssueContractBuilder.build());
            break;
          case "AccountUpdateContract":
            AccountUpdateContract.Builder accountUpdateContractBuilder = AccountUpdateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    accountUpdateContractBuilder, selfType);
            any = Any.pack(accountUpdateContractBuilder.build());
            break;
          case "FreezeBalanceContract":
            FreezeBalanceContract.Builder freezeBalanceContractBuilder = FreezeBalanceContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    freezeBalanceContractBuilder, selfType);
            any = Any.pack(freezeBalanceContractBuilder.build());
            break;
          case "UnfreezeBalanceContract":
            UnfreezeBalanceContract.Builder unfreezeBalanceContractBuilder = UnfreezeBalanceContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    unfreezeBalanceContractBuilder, selfType);
            any = Any.pack(unfreezeBalanceContractBuilder.build());
            break;
          case "WithdrawBalanceContract":
            WithdrawBalanceContract.Builder withdrawBalanceContractBuilder = WithdrawBalanceContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    withdrawBalanceContractBuilder, selfType);
            any = Any.pack(withdrawBalanceContractBuilder.build());
            break;
          case "UnfreezeAssetContract":
            UnfreezeAssetContract.Builder unfreezeAssetContractBuilder = UnfreezeAssetContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    unfreezeAssetContractBuilder, selfType);
            any = Any.pack(unfreezeAssetContractBuilder.build());
            break;
          case "UpdateAssetContract":
            UpdateAssetContract.Builder updateAssetContractBuilder = UpdateAssetContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    updateAssetContractBuilder, selfType);
            any = Any.pack(updateAssetContractBuilder.build());
            break;
          case "ProposalCreateContract":
            ProposalCreateContract.Builder createContractBuilder = ProposalCreateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    createContractBuilder, selfType);
            any = Any.pack(createContractBuilder.build());
            break;
          case "ProposalApproveContract":
            ProposalApproveContract.Builder approveContractBuilder = ProposalApproveContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    approveContractBuilder, selfType);
            any = Any.pack(approveContractBuilder.build());
            break;
          case "ProposalDeleteContract":
            ProposalDeleteContract.Builder deleteContractBuilder = ProposalDeleteContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    deleteContractBuilder, selfType);
            any = Any.pack(deleteContractBuilder.build());
            break;
//          case "SetAccountIdContract":
//            Contract.SetAccountIdContract.Builder setAccountid = Contract.SetAccountIdContract
//                    .newBuilder();
//            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
//                    setAccountid, selfType);
//            any = Any.pack(setAccountid.build());
//            break;
          case "CreateSmartContract":
            CreateSmartContract.Builder createSmartContractBuilder = CreateSmartContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    createSmartContractBuilder, selfType);
            any = Any.pack(createSmartContractBuilder.build());
            break;
          case "TriggerSmartContract":
            TriggerSmartContract.Builder triggerSmartContractBuilder = TriggerSmartContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    triggerSmartContractBuilder, selfType);

            any = Any.pack(triggerSmartContractBuilder.build());
            break;
          case "UpdateSettingContract":
            UpdateSettingContract.Builder updateSettingContractBuilder = UpdateSettingContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    updateSettingContractBuilder, selfType);
            any = Any.pack(updateSettingContractBuilder.build());
            break;
          case "ExchangeCreateContract":
            ExchangeCreateContract.Builder exchangeCreateContractBuilder = ExchangeCreateContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    exchangeCreateContractBuilder, selfType);
            any = Any.pack(exchangeCreateContractBuilder.build());
            break;
          case "ExchangeInjectContract":
            ExchangeInjectContract.Builder exchangeInjectContractBuilder = ExchangeInjectContract
                    .newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    exchangeInjectContractBuilder, selfType);
            any = Any.pack(exchangeInjectContractBuilder.build());
            break;
          case "ExchangeTransactionContract":
            ExchangeTransactionContract.Builder exchangeTransactionContractBuilder =
                    ExchangeTransactionContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    exchangeTransactionContractBuilder, selfType);
            any = Any.pack(exchangeTransactionContractBuilder.build());
            break;
          case "ExchangeWithdrawContract":
            ExchangeWithdrawContract.Builder exchangeWithdrawContractBuilder =
                    ExchangeWithdrawContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    exchangeWithdrawContractBuilder, selfType);
            any = Any.pack(exchangeWithdrawContractBuilder.build());
            break;
          case "UpdateEnergyLimitContract":
            UpdateEnergyLimitContract.Builder updateEnergyLimitContractBuilder =
                    UpdateEnergyLimitContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    updateEnergyLimitContractBuilder, selfType);
            any = Any.pack(updateEnergyLimitContractBuilder.build());
            break;
          case "AccountPermissionUpdateContract":
            AccountPermissionUpdateContract.Builder accountPermissionUpdateContractBuilder =
                    AccountPermissionUpdateContract.newBuilder();
            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(),
                    accountPermissionUpdateContractBuilder, selfType);
            any = Any.pack(accountPermissionUpdateContractBuilder.build());
            break;
//          case "ClearABIContract":
//            Contract.ClearABIContract.Builder clearABIContract =
//                    Contract.ClearABIContract.newBuilder();
//            JsonFormat.merge(parameter.getJSONObject(VALUE).toJSONString(), clearABIContract,
//                    selfType);
//            any = Any.pack(clearABIContract.build());
//            break;
          // todo add other contract
          default:
        }
        if (any != null) {
          String value = ByteArray.toHexString(any.getValue().toByteArray());
          parameter.put(VALUE, value);
          contract.put("parameter", parameter);
          contracts.add(contract);
        }
      } catch (JsonFormat.ParseException e) {
        System.out.println(e.getMessage());
      }
    }

    rawData.put("contract", contracts);
    jsonTransaction.put("raw_data", rawData);
    Transaction.Builder transactionBuilder = Transaction.newBuilder();
    try {
      JsonFormat.merge(jsonTransaction.toJSONString(), transactionBuilder, selfType);
      return transactionBuilder.build();
    } catch (JsonFormat.ParseException e) {
      System.out.println(e.getMessage());
      return null;
    }
  }


  public static String printDelegatedResourceList(DelegatedResourceList delegatedResourceList) {
    String result = "" + delegatedResourceList.getDelegatedResourceCount() + "\n";
    result += "DelegatedResourceList: [ \n";
    for (DelegatedResource delegatedResource : delegatedResourceList.getDelegatedResourceList()) {
      result += printDelegatedResource(delegatedResource);
      result += "\n";
    }
    result += "]";
    return result;
  }

  public static String printDelegatedResourceAccountIndex(
      DelegatedResourceAccountIndex delegatedResourceAccountIndex) {

    String result = "";
    result += "address: ";
    result += TronWallet._encode58Check(delegatedResourceAccountIndex.getAccount().toByteArray());

    result += "from: [ \n";
    for (ByteString fromAddress : delegatedResourceAccountIndex.getFromAccountsList()) {
      result += TronWallet._encode58Check(fromAddress.toByteArray());
      result += "\n";
    }
    result += "]";

    result += "to: [ \n";
    for (ByteString toAddress : delegatedResourceAccountIndex.getToAccountsList()) {
      result += TronWallet._encode58Check(toAddress.toByteArray());
      result += "\n";
    }
    result += "]";
    return result;
  }


  public static String printDelegatedResource(DelegatedResource delegatedResource) {
    String result = "";
    result += "from: ";
    result += TronWallet._encode58Check(delegatedResource.getFrom().toByteArray());
    result += "\n";
    result += "to: ";
    result += TronWallet._encode58Check(delegatedResource.getTo().toByteArray());
    result += "\n";
    result += "frozenBalanceForBandwidth: ";
    result += delegatedResource.getFrozenBalanceForBandwidth();
    result += "\n";
    result += "expireTimeForBandwidth: ";
    result += delegatedResource.getExpireTimeForBandwidth();
    result += "\n";
    result += "frozenBalanceForEnergy: ";
    result += delegatedResource.getFrozenBalanceForEnergy();
    result += "\n";
    result += "expireTimeForEnergy: ";
    result += delegatedResource.getExpireTimeForEnergy();
    result += "\n";
    return result;
  }

  public static String printExchange(Exchange exchange) {
    String result = "";
    result += "id: ";
    result += exchange.getExchangeId();
    result += "\n";
    result += "creator: ";
    result += TronWallet._encode58Check(exchange.getCreatorAddress().toByteArray());
    result += "\n";
    result += "createTime: ";
    result += exchange.getCreateTime();
    result += "\n";
    result += "firstTokenId: ";
    result += exchange.getFirstTokenId().toStringUtf8();
    result += "\n";
    result += "firstTokenBalance: ";
    result += exchange.getFirstTokenBalance();
    result += "\n";
    result += "secondTokenId: ";
    result += exchange.getSecondTokenId().toStringUtf8();
    result += "\n";
    result += "secondTokenBalance: ";
    result += exchange.getSecondTokenBalance();
    result += "\n";
    return result;
  }

  public static String printExchangeList(ExchangeList exchangeList) {
    String result = "\n";
    int i = 0;
    for (Exchange exchange : exchangeList.getExchangesList()) {
      result += "exchange " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printExchange(exchange);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printChainParameters(ChainParameters chainParameters) {
    String result = "\n";
    result += "ChainParameters : \n";
    for (ChainParameter para : chainParameters.getChainParameterList()) {
      result += para.getKey() + " : " + para.getValue();
      result += "\n";
    }
    return result;
  }

  public static String printWitnessList(WitnessList witnessList) {
    String result = "\n";
    int i = 0;
    for (Witness witness : witnessList.getWitnessesList()) {
      result += "witness " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printWitness(witness);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }


  public static String printAssetIssue(AssetIssueContract assetIssue) {
    String result = "";
    result += "id: ";
    result += assetIssue.getId();
    result += "\n";
    result += "owner_address: ";
    result += TronWallet._encode58Check(assetIssue.getOwnerAddress().toByteArray());
    result += "\n";
    result += "name: ";
    result += new String(assetIssue.getName().toByteArray(), Charset.forName("UTF-8"));
    result += "\n";
    result += "order: ";
    result += assetIssue.getOrder();
    result += "\n";
    result += "total_supply: ";
    result += assetIssue.getTotalSupply();
    result += "\n";
    result += "trx_num: ";
    result += assetIssue.getTrxNum();
    result += "\n";
    result += "num: ";
    result += assetIssue.getNum();
    result += "\n";
    result += "precision ";
    result += assetIssue.getPrecision();
    result += "\n";
    result += "start_time: ";
    result += new Date(assetIssue.getStartTime());
    result += "\n";
    result += "end_time: ";
    result += new Date(assetIssue.getEndTime());
    result += "\n";
    result += "vote_score: ";
    result += assetIssue.getVoteScore();
    result += "\n";
    result += "description: ";
    result += new String(assetIssue.getDescription().toByteArray(), Charset.forName("UTF-8"));
    result += "\n";
    result += "url: ";
    result += new String(assetIssue.getUrl().toByteArray(), Charset.forName("UTF-8"));
    result += "\n";
    result += "free asset net limit: ";
    result += assetIssue.getFreeAssetNetLimit();
    result += "\n";
    result += "public free asset net limit: ";
    result += assetIssue.getPublicFreeAssetNetLimit();
    result += "\n";
    result += "public free asset net usage: ";
    result += assetIssue.getPublicFreeAssetNetUsage();
    result += "\n";
    result += "public latest free net time: ";
    result += assetIssue.getPublicLatestFreeNetTime();
    result += "\n";

    if (assetIssue.getFrozenSupplyCount() > 0) {
      for (FrozenSupply frozenSupply : assetIssue.getFrozenSupplyList()) {
        result += "frozen_supply";
        result += "\n";
        result += "{";
        result += "\n";
        result += "  amount: ";
        result += frozenSupply.getFrozenAmount();
        result += "\n";
        result += "  frozen_days: ";
        result += frozenSupply.getFrozenDays();
        result += "\n";
        result += "}";
        result += "\n";
      }
    }

    if (assetIssue.getId().equals("")) {
      result += "\n";
      result += "Note: In 3.2, you can use getAssetIssueById or getAssetIssueListByName, because 3.2 allows same token name.";
      result += "\n";
    }
    return result;
  }

  public static String printAssetIssueList(AssetIssueList assetIssueList) {
    String result = "\n";
    int i = 0;
    for (AssetIssueContract assetIssue : assetIssueList.getAssetIssueList()) {
      result += "assetIssue " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printAssetIssue(assetIssue);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printContract(Transaction.Contract contract) {
    String result = "";
    try {
      result += "contract_type: ";
      result += contract.getType();
      result += "\n";

      switch (contract.getType()) {
        case AccountCreateContract:
          AccountCreateContract accountCreateContract = contract.getParameter()
              .unpack(AccountCreateContract.class);
          result += "type: ";
          result += accountCreateContract.getType();
          result += "\n";
          if (accountCreateContract.getAccountAddress() != null
              && !accountCreateContract.getAccountAddress().isEmpty()) {
            result += "account_address: ";
            result += TronWallet._encode58Check(accountCreateContract.getAccountAddress().toByteArray());
            result += "\n";
          }
          result += "owner_address: ";
          result += TronWallet._encode58Check(accountCreateContract.getOwnerAddress().toByteArray());
          result += "\n";
          break;
        case AccountUpdateContract:
          AccountUpdateContract accountUpdateContract = contract.getParameter()
              .unpack(AccountUpdateContract.class);
          if (accountUpdateContract.getAccountName() != null
              && !accountUpdateContract.getAccountName().isEmpty()) {
            result += "account_name: ";
            result += new String(accountUpdateContract.getAccountName().toByteArray(),
                Charset.forName("UTF-8"));
            result += "\n";
          }
          result += "owner_address: ";
          result += TronWallet._encode58Check(accountUpdateContract.getOwnerAddress().toByteArray());
          result += "\n";
          break;
        case TransferContract:
          TransferContract transferContract = contract.getParameter()
              .unpack(TransferContract.class);
          result += "owner_address: ";
          result += TronWallet._encode58Check(transferContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "to_address: ";
          result += TronWallet._encode58Check(transferContract.getToAddress().toByteArray());
          result += "\n";
          result += "amount: ";
          result += transferContract.getAmount();
          result += "\n";
          break;
        case TransferAssetContract:
          TransferAssetContract transferAssetContract = contract.getParameter()
              .unpack(TransferAssetContract.class);
          result += "asset_name: ";
          result += new String(transferAssetContract.getAssetName().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          result += "owner_address: ";
          result += TronWallet._encode58Check(transferAssetContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "to_address: ";
          result += TronWallet._encode58Check(transferAssetContract.getToAddress().toByteArray());
          result += "\n";
          result += "amount: ";
          result += transferAssetContract.getAmount();
          result += "\n";
          break;
        case VoteAssetContract:
          VoteAssetContract voteAssetContract = contract.getParameter()
              .unpack(VoteAssetContract.class);
          break;
        case VoteWitnessContract:
          VoteWitnessContract voteWitnessContract = contract.getParameter()
              .unpack(VoteWitnessContract.class);
          result += "owner_address: ";
          result += TronWallet._encode58Check(voteWitnessContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "votes: ";
          result += "\n";
          result += "{";
          result += "\n";
          for (VoteWitnessContract.Vote vote : voteWitnessContract.getVotesList()) {
            result += "[";
            result += "\n";
            result += "vote_address: ";
            result += TronWallet._encode58Check(vote.getVoteAddress().toByteArray());
            result += "\n";
            result += "vote_count: ";
            result += vote.getVoteCount();
            result += "\n";
            result += "]";
            result += "\n";
          }
          result += "}";
          result += "\n";
          break;
        case WitnessCreateContract:
          WitnessCreateContract witnessCreateContract = contract.getParameter()
              .unpack(WitnessCreateContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(witnessCreateContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "url: ";
          result += new String(witnessCreateContract.getUrl().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          break;
        case WitnessUpdateContract:
          WitnessUpdateContract witnessUpdateContract = contract.getParameter()
              .unpack(WitnessUpdateContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(witnessUpdateContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "url: ";
          result += new String(witnessUpdateContract.getUpdateUrl().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          break;
        case AssetIssueContract:
          AssetIssueContract assetIssueContract = contract.getParameter()
              .unpack(AssetIssueContract.class);
          result += printAssetIssue(assetIssueContract);
          break;
        case UpdateAssetContract:
          UpdateAssetContract updateAssetContract = contract.getParameter()
              .unpack(UpdateAssetContract.class);
          result += "owner_address: ";
          result += TronWallet._encode58Check(updateAssetContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "description: ";
          result += new String(updateAssetContract.getDescription().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          result += "url: ";
          result += new String(updateAssetContract.getUrl().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          result += "free asset net limit: ";
          result += updateAssetContract.getNewLimit();
          result += "\n";
          result += "public free asset net limit: ";
          result += updateAssetContract.getNewPublicLimit();
          result += "\n";
          break;
        case ParticipateAssetIssueContract:
          ParticipateAssetIssueContract participateAssetIssueContract = contract.getParameter()
              .unpack(ParticipateAssetIssueContract.class);
          result += "asset_name: ";
          result += new String(participateAssetIssueContract.getAssetName().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(participateAssetIssueContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "to_address: ";
          result += TronWallet
              ._encode58Check(participateAssetIssueContract.getToAddress().toByteArray());
          result += "\n";
          result += "amount: ";
          result += participateAssetIssueContract.getAmount();
          result += "\n";
          break;
        case FreezeBalanceContract:
          FreezeBalanceContract freezeBalanceContract = contract.getParameter()
              .unpack(FreezeBalanceContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(freezeBalanceContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "receive_address: ";
          result += TronWallet
              ._encode58Check(freezeBalanceContract.getReceiverAddress().toByteArray());
          result += "\n";
          result += "frozen_balance: ";
          result += freezeBalanceContract.getFrozenBalance();
          result += "\n";
          result += "frozen_duration: ";
          result += freezeBalanceContract.getFrozenDuration();
          result += "\n";
          break;
        case UnfreezeBalanceContract:
          UnfreezeBalanceContract unfreezeBalanceContract = contract.getParameter()
              .unpack(UnfreezeBalanceContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(unfreezeBalanceContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "receive_address: ";
          result += TronWallet
              ._encode58Check(unfreezeBalanceContract.getReceiverAddress().toByteArray());
          result += "\n";
          break;
        case UnfreezeAssetContract:
          UnfreezeAssetContract unfreezeAssetContract = contract.getParameter()
              .unpack(UnfreezeAssetContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(unfreezeAssetContract.getOwnerAddress().toByteArray());
          result += "\n";
          break;
        case WithdrawBalanceContract:
          WithdrawBalanceContract withdrawBalanceContract = contract.getParameter()
              .unpack(WithdrawBalanceContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(withdrawBalanceContract.getOwnerAddress().toByteArray());
          result += "\n";
          break;
        case SetAccountIdContract:
          SetAccountIdContract setAccountIdContract = contract.getParameter()
              .unpack(SetAccountIdContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(setAccountIdContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "account_id: ";
          result += new String(setAccountIdContract.getAccountId().toByteArray(),
              Charset.forName("UTF-8"));
          result += "\n";
          break;
        case CreateSmartContract:
          CreateSmartContract createSmartContract = contract.getParameter()
              .unpack(CreateSmartContract.class);
          SmartContract newContract = createSmartContract.getNewContract();
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(createSmartContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "ABI: ";
          result += newContract.getAbi().toString();
          result += "\n";
          result += "byte_code: ";
          result += Hex.toHexString(newContract.getBytecode().toByteArray());
          result += "\n";
          result += "call_value: ";
          result += newContract.getCallValue();
          result += "\n";
          result += "contract_address:";
          result += TronWallet._encode58Check(newContract.getContractAddress().toByteArray());
          result += "\n";
          break;
        case TriggerSmartContract:
          TriggerSmartContract triggerSmartContract = contract.getParameter()
              .unpack(TriggerSmartContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(triggerSmartContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "contract_address: ";
          result += TronWallet
              ._encode58Check(triggerSmartContract.getContractAddress().toByteArray());
          result += "\n";
          result += "call_value:";
          result += triggerSmartContract.getCallValue();
          result += "\n";
          result += "data:";
          result += Hex.toHexString(triggerSmartContract.getData().toByteArray());
          result += "\n";
          break;
        case ProposalCreateContract:
          ProposalCreateContract proposalCreateContract = contract.getParameter()
              .unpack(ProposalCreateContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(proposalCreateContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "parametersMap: ";
          result += proposalCreateContract.getParametersMap();
          result += "\n";
          break;
        case ProposalApproveContract:
          ProposalApproveContract proposalApproveContract = contract.getParameter()
              .unpack(ProposalApproveContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(proposalApproveContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "proposal id: ";
          result += proposalApproveContract.getProposalId();
          result += "\n";
          result += "IsAddApproval: ";
          result += proposalApproveContract.getIsAddApproval();
          result += "\n";
          break;
        case ProposalDeleteContract:
          ProposalDeleteContract proposalDeleteContract = contract.getParameter()
              .unpack(ProposalDeleteContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(proposalDeleteContract.getOwnerAddress().toByteArray());
          break;
        case ExchangeCreateContract:
          ExchangeCreateContract exchangeCreateContract = contract.getParameter()
              .unpack(ExchangeCreateContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(exchangeCreateContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "firstTokenId: ";
          result += exchangeCreateContract.getFirstTokenId().toStringUtf8();
          result += "\n";
          result += "firstTokenBalance: ";
          result += exchangeCreateContract.getFirstTokenBalance();
          result += "\n";
          result += "secondTokenId: ";
          result += exchangeCreateContract.getSecondTokenId().toStringUtf8();
          result += "\n";
          result += "secondTokenBalance: ";
          result += exchangeCreateContract.getSecondTokenBalance();
          result += "\n";
          break;
        case ExchangeInjectContract:
          ExchangeInjectContract exchangeInjectContract = contract.getParameter()
              .unpack(ExchangeInjectContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(exchangeInjectContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "TokenId: ";
          result += exchangeInjectContract.getTokenId().toStringUtf8();
          result += "\n";
          result += "quant: ";
          result += exchangeInjectContract.getQuant();
          result += "\n";
          break;
        case ExchangeWithdrawContract:
          ExchangeWithdrawContract exchangeWithdrawContract = contract.getParameter()
              .unpack(ExchangeWithdrawContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(exchangeWithdrawContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "TokenId: ";
          result += exchangeWithdrawContract.getTokenId().toStringUtf8();
          result += "\n";
          result += "quant: ";
          result += exchangeWithdrawContract.getQuant();
          result += "\n";
          break;
        case ExchangeTransactionContract:
          ExchangeTransactionContract exchangeTransactionContract = contract.getParameter()
              .unpack(ExchangeTransactionContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(exchangeTransactionContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "TokenId: ";
          result += exchangeTransactionContract.getTokenId().toStringUtf8();
          result += "\n";
          result += "quant: ";
          result += exchangeTransactionContract.getQuant();
          result += "\n";
          break;
        case AccountPermissionUpdateContract:
          AccountPermissionUpdateContract accountPermissionUpdateContract = contract.getParameter()
              .unpack(AccountPermissionUpdateContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(accountPermissionUpdateContract.getOwnerAddress().toByteArray());
          result += "\n";
          if (accountPermissionUpdateContract.hasOwner()) {
            result += "owner_permission: ";
            result += "\n";
            result += "{";
            result += "\n";
            result += printPermission(accountPermissionUpdateContract.getOwner());
            result += "\n";
            result += "}";
            result += "\n";
          }

          if (accountPermissionUpdateContract.hasWitness()) {
            result += "witness_permission: ";
            result += "\n";
            result += "{";
            result += "\n";
            result += printPermission(accountPermissionUpdateContract.getWitness());
            result += "\n";
            result += "}";
            result += "\n";
          }

          if (accountPermissionUpdateContract.getActivesCount() > 0) {
            result += "active_permissions: ";
            result += printPermissionList(accountPermissionUpdateContract.getActivesList());
            result += "\n";
          }
          break;
        case UpdateSettingContract:
          UpdateSettingContract updateSettingContract = contract.getParameter()
              .unpack(UpdateSettingContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(updateSettingContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "contract_address: ";
          result += TronWallet
              ._encode58Check(updateSettingContract.getContractAddress().toByteArray());
          result += "\n";
          result += "consume_user_resource_percent: ";
          result += updateSettingContract.getConsumeUserResourcePercent();
          result += "\n";
          break;
        case UpdateEnergyLimitContract:
          UpdateEnergyLimitContract updateEnergyLimitContract = contract.getParameter()
              .unpack(UpdateEnergyLimitContract.class);
          result += "owner_address: ";
          result += TronWallet
              ._encode58Check(updateEnergyLimitContract.getOwnerAddress().toByteArray());
          result += "\n";
          result += "contract_address: ";
          result += TronWallet
              ._encode58Check(updateEnergyLimitContract.getContractAddress().toByteArray());
          result += "\n";
          result += "origin_energy_limit: ";
          result += updateEnergyLimitContract.getOriginEnergyLimit();
          result += "\n";
          break;
        // case BuyStorageContract:
        //   BuyStorageContract buyStorageContract = contract.getParameter()
        //       .unpack(BuyStorageContract.class);
        //   result += "owner_address: ";
        //   result += TronWallet
        //       ._encode58Check(buyStorageContract.getOwnerAddress().toByteArray());
        //   result += "\n";
        //   result += "quant:";
        //   result += buyStorageContract.getQuant();
        //   result += "\n";
        //   break;
        // case SellStorageContract:
        //   SellStorageContract sellStorageContract = contract.getParameter()
        //       .unpack(SellStorageContract.class);
        //   result += "owner_address: ";
        //   result += TronWallet
        //       ._encode58Check(sellStorageContract.getOwnerAddress().toByteArray());
        //   result += "\n";
        //   result += "storageBytes:";
        //   result += sellStorageContract.getStorageBytes();
        //   result += "\n";
        //   break;
        default:
          return "";
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return "";
    }
    return result;
  }

  public static String printContractList(List<Contract> contractList) {
    String result = "";
    int i = 0;
    for (Contract contract : contractList) {
      result += "contract " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printContract(contract);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printTransactionRow(Transaction.raw raw) {
    String result = "";

    if (raw.getRefBlockBytes() != null) {
      result += "ref_block_bytes: ";
      result += ByteArray.toHexString(raw.getRefBlockBytes().toByteArray());
      result += "\n";
    }

    if (raw.getRefBlockHash() != null) {
      result += "ref_block_hash: ";
      result += ByteArray.toHexString(raw.getRefBlockHash().toByteArray());
      result += "\n";
    }

    if (raw.getContractCount() > 0) {
      result += "contract: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printContractList(raw.getContractList());
      result += "}";
      result += "\n";
    }

    result += "timestamp: ";
    result += new Date(raw.getTimestamp());
    result += "\n";

    result += "fee_limit: ";
    result += raw.getFeeLimit();
    result += "\n";

    return result;
  }

  public static String printSignature(List<ByteString> signatureList) {
    String result = "";
    int i = 0;
    for (ByteString signature : signatureList) {
      result += "signature " + i + " :";
      result += ByteArray.toHexString(signature.toByteArray());
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printRet(List<Result> resultList) {
    String results = "";
    int i = 0;
    for (Result result : resultList) {
      results += "result: ";
      results += i;
      results += " ::: ";
      results += "\n";
      results += "[";
      results += "\n";
      results += "code ::: ";
      results += result.getRet();
      results += "\n";
      results += "fee ::: ";
      results += result.getFee();
      results += "\n";
      results += "ContractRet ::: ";
      results += result.getContractRet().name();
      results += "\n";
      results += "]";
      results += "\n";
      i++;
    }
    return results;
  }

  public static Map decodeTransaction(Transaction transaction) {
    Map<String, String> decodedTR = new HashMap<String, String>();

    decodedTR.put("hash", ByteArray.toHexString(Sha256Hash.hash(transaction.toByteArray())));
    decodedTR.put("txID", ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray())));
    decodedTR.put("raw_data", printTransactionRow(transaction.getRawData()));
    decodedTR.put("signature", printSignature(transaction.getSignatureList()));
    decodedTR.put("ret", printRet(transaction.getRetList()));

    return decodedTR;
  }


  public static String printTransaction(Transaction transaction) {
    String result = "";
    result += "hash: ";
    result += "\n";
    result += ByteArray.toHexString(Sha256Hash.hash(transaction.toByteArray()));
    result += "\n";
    result += "txid: ";
    result += "\n";
    result += ByteArray.toHexString(Sha256Hash.hash(transaction.getRawData().toByteArray()));
    result += "\n";

    if (transaction.getRawData() != null) {
      result += "raw_data: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printTransactionRow(transaction.getRawData());
      result += "}";
      result += "\n";
    }
    if (transaction.getSignatureCount() > 0) {
      result += "signature: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printSignature(transaction.getSignatureList());
      result += "}";
      result += "\n";
    }
    if (transaction.getRetCount() != 0) {
      result += "ret: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printRet(transaction.getRetList());
      result += "}";
      result += "\n";
    }
    return result;
  }

  public static String printTransaction(TransactionExtention transactionExtention) {
    String result = "";
    result += "txid: ";
    result += "\n";
    result += ByteArray.toHexString(transactionExtention.getTxid().toByteArray());
    result += "\n";

    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRawData() != null) {
      result += "raw_data: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printTransactionRow(transaction.getRawData());
      result += "}";
      result += "\n";
    }
    if (transaction.getSignatureCount() > 0) {
      result += "signature: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printSignature(transaction.getSignatureList());
      result += "}";
      result += "\n";
    }
    if (transaction.getRetCount() != 0) {
      result += "ret: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printRet(transaction.getRetList());
      result += "}";
      result += "\n";
    }
    return result;
  }
//
//  public static String printTransactionInfo(TransactionInfo transactionInfo) {
//    String result = "";
//    result += "txid: ";
//    result += "\n";
//    result += ByteArray.toHexString(transactionInfo.getId().toByteArray());
//    result += "\n";
//    result += "fee: ";
//    result += "\n";
//    result += transactionInfo.getFee();
//    result += "\n";
//    result += "blockNumber: ";
//    result += "\n";
//    result += transactionInfo.getBlockNumber();
//    result += "\n";
//    result += "blockTimeStamp: ";
//    result += "\n";
//    result += transactionInfo.getBlockTimeStamp();
//    result += "\n";
//    result += "result: ";
//    result += "\n";
//    if (transactionInfo.getResult().equals(code.SUCESS)) {
//      result += "SUCCESS";
//    } else {
//      result += "FAILED";
//    }
//    result += "\n";
//    result += "resMessage: ";
//    result += "\n";
//    result += ByteArray.toStr(transactionInfo.getResMessage().toByteArray());
//    result += "\n";
//    result += "contractResult: ";
//    result += "\n";
//    result += ByteArray.toHexString(transactionInfo.getContractResult(0).toByteArray());
//    result += "\n";
//    result += "contractAddress: ";
//    result += "\n";
//    result += TronWallet._encode58Check(transactionInfo.getContractAddress().toByteArray());
//    result += "\n";
//    result += "logList: ";
//    result += "\n";
//    result += printLogList(transactionInfo.getLogList());
//    result += "\n";
//    result += "receipt: ";
//    result += "\n";
//    result += printReceipt(transactionInfo.getReceipt());
//    result += "\n";
//    if (transactionInfo.getUnfreezeAmount() != 0) {
//      result += "UnfreezeAmount: ";
//      result += transactionInfo.getUnfreezeAmount();
//      result += "\n";
//    }
//    if (transactionInfo.getWithdrawAmount() != 0) {
//      result += "WithdrawAmount: ";
//      result += transactionInfo.getWithdrawAmount();
//      result += "\n";
//    }
//    if (transactionInfo.getExchangeReceivedAmount() != 0) {
//      result += "ExchangeReceivedAmount: ";
//      result += transactionInfo.getExchangeReceivedAmount();
//      result += "\n";
//    }
//    if (transactionInfo.getExchangeInjectAnotherAmount() != 0) {
//      result += "ExchangeInjectAnotherAmount: ";
//      result += transactionInfo.getExchangeInjectAnotherAmount();
//      result += "\n";
//    }
//    if (transactionInfo.getExchangeWithdrawAnotherAmount() != 0) {
//      result += "ExchangeWithdrawAnotherAmount: ";
//      result += transactionInfo.getExchangeWithdrawAnotherAmount();
//      result += "\n";
//    }
//    if (transactionInfo.getExchangeId() != 0) {
//      result += "ExchangeId: ";
//      result += transactionInfo.getExchangeId();
//      result += "\n";
//    }
//    result += "InternalTransactionList: ";
//    result += "\n";
//    result += printInternalTransactionList(transactionInfo.getInternalTransactionsList());
//    result += "\n";
//    return result;
//  }

//  public static String printInternalTransactionList(
//      List<InternalTransaction> internalTransactions) {
//    StringBuilder result = new StringBuilder("");
//    internalTransactions.forEach(internalTransaction -> {
//          result.append("[\n");
//          result.append("  hash:\n");
//          result.append("  " + ByteArray.toHexString(internalTransaction.getHash().toByteArray()));
//          result.append("  \n");
//          result.append("  caller_address:\n");
//          result.append(
//              "  " + ByteArray.toHexString(internalTransaction.getCallerAddress().toByteArray()));
//          result.append("  \n");
//          result.append("  transferTo_address:\n");
//          result.append(
//              "  " + ByteArray.toHexString(internalTransaction.getTransferToAddress().toByteArray()));
//          result.append("  \n");
//          result.append("  callValueInfo:\n");
//          StringBuilder callValueInfo = new StringBuilder("");
//
//          internalTransaction.getCallValueInfoList().forEach(token -> {
//            callValueInfo.append("  [\n");
//            callValueInfo.append("    TokenName(Default trx):\n");
//            if (null == token.getTokenId() || token.getTokenId().length() == 0) {
//              callValueInfo.append("    TRX(SUN)");
//            } else {
//              callValueInfo.append("    " + token.getTokenId());
//            }
//            callValueInfo.append("    \n");
//            callValueInfo.append("    callValue:\n");
//            callValueInfo.append("    " + token.getCallValue());
//            callValueInfo.append("  \n");
//            callValueInfo.append("  ]\n");
//            callValueInfo.append("    \n");
//          });
//          result.append(callValueInfo);
//          result.append("  note:\n");
//          result.append("  " + new String(internalTransaction.getNote().toByteArray()));
//          result.append("  \n");
//          result.append("  rejected:\n");
//          result.append("  " + internalTransaction.getRejected());
//          result.append("  \n");
//          result.append("]\n");
//        }
//    );
//
//    return result.toString();
//
//  }

  private static String printReceipt(ResourceReceipt receipt) {
    String result = "";
    result += "EnergyUsage: ";
    result += "\n";
    result += receipt.getEnergyUsage();
    result += "\n";
    result += "EnergyFee(SUN): ";
    result += "\n";
    result += receipt.getEnergyFee();
    result += "\n";
    result += "OriginEnergyUsage: ";
    result += "\n";
    result += receipt.getOriginEnergyUsage();
    result += "\n";
    result += "EnergyUsageTotal: ";
    result += "\n";
    result += receipt.getEnergyUsageTotal();
    result += "\n";
    result += "NetUsage: ";
    result += "\n";
    result += receipt.getNetUsage();
    result += "\n";
    result += "NetFee: ";
    result += "\n";
    result += receipt.getNetFee();
    result += "\n";
    return result;
  }

//  public static String printLogList(List<Log> logList) {
//    StringBuilder result = new StringBuilder("");
//    logList.forEach(log -> {
//          result.append("address:\n");
//          result.append(ByteArray.toHexString(log.getAddress().toByteArray()));
//          result.append("\n");
//          result.append("data:\n");
//          result.append(ByteArray.toHexString(log.getData().toByteArray()));
//          result.append("\n");
//          result.append("TopicsList\n");
//          StringBuilder topics = new StringBuilder("");
//
//          log.getTopicsList().forEach(bytes -> {
//            topics.append(ByteArray.toHexString(bytes.toByteArray()));
//            topics.append("\n");
//          });
//          result.append(topics);
//        }
//    );
//
//    return result.toString();
//  }

  public static String printTransactionList(TransactionList transactionList) {
    return printTransactions(transactionList.getTransactionList());
  }

  public static String printTransactionList(TransactionListExtention transactionList) {
    return printTransactionsExt(transactionList.getTransactionList());
  }

  public static String printTransactions(List<Transaction> transactionList) {
    String result = "\n";
    int i = 0;
    for (Transaction transaction : transactionList) {
      result += "transaction " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printTransaction(transaction);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printTransactionsExt(List<TransactionExtention> transactionList) {
    String result = "\n";
    int i = 0;
    for (TransactionExtention transaction : transactionList) {
      result += "transaction " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printTransaction(transaction);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printBlockRow(BlockHeader.raw raw) {
    String result = "";

    result += "timestamp: ";
    result += new Date(raw.getTimestamp());
    result += "\n";

    result += "txTrieRoot: ";
    result += ByteArray.toHexString(raw.getTxTrieRoot().toByteArray());
    result += "\n";

    result += "parentHash: ";
    result += ByteArray.toHexString(raw.getParentHash().toByteArray());
    result += "\n";

    result += "number: ";
    result += raw.getNumber();
    result += "\n";

    result += "witness_id: ";
    result += raw.getWitnessId();
    result += "\n";

    result += "witness_address: ";
    result += TronWallet._encode58Check(raw.getWitnessAddress().toByteArray());
    result += "\n";

    result += "version: ";
    result += raw.getVersion();
    result += "\n";

    return result;
  }


  public static String printBlockHeader(BlockHeader blockHeader) {
    String result = "";
    result += "raw_data: ";
    result += "\n";
    result += "{";
    result += "\n";
    result += printBlockRow(blockHeader.getRawData());
    result += "}";
    result += "\n";

    result += "witness_signature: ";
    result += "\n";
    result += ByteArray.toHexString(blockHeader.getWitnessSignature().toByteArray());
    result += "\n";
    return result;
  }

  public static String printBlock(Block block) {
    String result = "\n";
    if (block.getBlockHeader() != null) {
      result += "block_header: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printBlockHeader(block.getBlockHeader());
      result += "}";
      result += "\n";
    }
    if (block.getTransactionsCount() > 0) {
      result += printTransactions(block.getTransactionsList());
    }
    return result;
  }

  public static String printBlockExtention(BlockExtention blockExtention) {
    String result = "\n";
    if (blockExtention.getBlockid() != null) {
      result += "block_id: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += ByteArray.toHexString(blockExtention.getBlockid().toByteArray());
      result += "\n";
      result += "}";
      result += "\n";
    }
    if (blockExtention.getBlockHeader() != null) {
      result += "block_header: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printBlockHeader(blockExtention.getBlockHeader());
      result += "}";
      result += "\n";
    }
    if (blockExtention.getTransactionsCount() > 0) {
      result += printTransactionsExt(blockExtention.getTransactionsList());
    }
    return result;
  }

  public static String printBlockList(BlockList blockList) {
    String result = "\n";
    int i = 0;
    for (Block block : blockList.getBlockList()) {
      result += "block " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printBlock(block);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printBlockList(BlockListExtention blockList) {
    String result = "\n";
    int i = 0;
    for (BlockExtention block : blockList.getBlockList()) {
      result += "block " + i + " :::";
      result += "\n";
      result += "[";
      result += "\n";
      result += printBlockExtention(block);
      result += "]";
      result += "\n";
      result += "\n";
      i++;
    }
    return result;
  }

  public static String printAccountNet(AccountNetMessage accountNet) {
    String result = "";
    result += "free_net_used: ";
    result += accountNet.getFreeNetUsed();
    result += "\n";
    result += "free_net_limit: ";
    result += accountNet.getFreeNetLimit();
    result += "\n";
    result += "net_used: ";
    result += accountNet.getNetUsed();
    result += "\n";
    result += "net_limit: ";
    result += accountNet.getNetLimit();
    result += "\n";
    result += "total_net_limit: ";
    result += accountNet.getTotalNetLimit();
    result += "\n";
    result += "total_net_weight: ";
    result += accountNet.getTotalNetWeight();
    result += "\n";

    if (accountNet.getAssetNetLimitCount() > 0) {
      for (String name : accountNet.getAssetNetLimitMap().keySet()) {
        result += "asset";
        result += "\n";
        result += "{";
        result += "\n";
        result += "  name: ";
        result += name;
        result += "\n";
        result += "  free_asset_net_used: ";
        result += accountNet.getAssetNetUsedMap().get(name);
        result += "\n";
        result += "  free_asset_net_limit: ";
        result += accountNet.getAssetNetLimitMap().get(name);
        result += "\n";
        result += "}";
        result += "\n";
      }
    }
    return result;
  }


  public static String printAccountResourceMessage(AccountResourceMessage accountResourceMessage) {
    String result = "";
    result += "free_net_used: ";
    result += accountResourceMessage.getFreeNetUsed();
    result += "\n";
    result += "free_net_limit: ";
    result += accountResourceMessage.getFreeNetLimit();
    result += "\n";
    result += "net_used: ";
    result += accountResourceMessage.getNetUsed();
    result += "\n";
    result += "net_limit: ";
    result += accountResourceMessage.getNetLimit();
    result += "\n";
    result += "total_net_limit: ";
    result += accountResourceMessage.getTotalNetLimit();
    result += "\n";
    result += "total_net_weight: ";
    result += accountResourceMessage.getTotalNetWeight();
    result += "\n";

    result += "\n";
    result += "EnergyUsed: ";
    result += accountResourceMessage.getEnergyUsed();
    result += "\n";
    result += "EnergyLimit: ";
    result += accountResourceMessage.getEnergyLimit();
    result += "\n";
    result += "TotalEnergyLimit: ";
    result += accountResourceMessage.getTotalEnergyLimit();
    result += "\n";
    result += "TotalEnergyWeight: ";
    result += accountResourceMessage.getTotalEnergyWeight();
    result += "\n";

    if (accountResourceMessage.getAssetNetLimitCount() > 0) {
      for (String name : accountResourceMessage.getAssetNetLimitMap().keySet()) {
        result += "asset";
        result += "\n";
        result += "{";
        result += "\n";
        result += "  name: ";
        result += name;
        result += "\n";
        result += "  free_asset_net_used: ";
        result += accountResourceMessage.getAssetNetUsedMap().get(name);
        result += "\n";
        result += "  free_asset_net_limit: ";
        result += accountResourceMessage.getAssetNetLimitMap().get(name);
        result += "\n";
        result += "}";
        result += "\n";
      }
    }

    return result;
  }

  public static String printKey(Key key) {
    StringBuffer result = new StringBuffer();
    result.append("address: ");
    result.append(TronWallet._encode58Check(key.getAddress().toByteArray()));
    result.append("\n");
    result.append("weight: ");
    result.append(key.getWeight());
    result.append("\n");
    return result.toString();
  }

  public static String printPermissionList(List<Permission> permissionList) {
    String result = "\n";
    result += "[";
    result += "\n";
    int i = 0;
    for (Permission permission : permissionList) {
      result += "permission " + i + " :::";
      result += "\n";
      result += "{";
      result += "\n";
      result += printPermission(permission);
      result += "\n";
      result += "}";
      result += "\n";
      i++;
    }
    result += "]";
    return result;
  }

  public static String printPermission(Permission permission) {
    StringBuffer result = new StringBuffer();
    result.append("permission_type: ");
    result.append(permission.getType());
    result.append("\n");
    result.append("permission_id: ");
    result.append(permission.getId());
    result.append("\n");
    result.append("permission_name: ");
    result.append(permission.getPermissionName());
    result.append("\n");
    result.append("threshold: ");
    result.append(permission.getThreshold());
    result.append("\n");
    result.append("parent_id: ");
    result.append(permission.getParentId());
    result.append("\n");
    result.append("operations: ");
    result.append(ByteArray.toHexString(permission.getOperations().toByteArray()));
    result.append("\n");
    if (permission.getKeysCount() > 0) {
      result.append("keys:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (Key key : permission.getKeysList()) {
        result.append(printKey(key));
      }
      result.append("]");
      result.append("\n");
    }
    return result.toString();
  }

  public static String printResult(TransactionSignWeight.Result resul) {
    StringBuffer result = new StringBuffer();
    result.append("code: ");
    result.append(resul.getCode());
    result.append("\n");
    if (!Strings.isStringEmpty(resul.getMessage())) {
      result.append("message: ");
      result.append(resul.getMessage());
      result.append("\n");
    }
    return result.toString();
  }

  public static String printResult(TransactionApprovedList.Result resul) {
    StringBuffer result = new StringBuffer();
    result.append("code: ");
    result.append(resul.getCode());
    result.append("\n");
    if (!Strings.isStringEmpty(resul.getMessage())) {
      result.append("message: ");
      result.append(resul.getMessage());
      result.append("\n");
    }
    return result.toString();
  }

  public static String printTransactionSignWeight(TransactionSignWeight transactionSignWeight) {
    StringBuffer result = new StringBuffer();
    result.append("permission:");
    result.append("\n");
    result.append("{");
    result.append("\n");
    result.append(printPermission(transactionSignWeight.getPermission()));
    result.append("}");
    result.append("\n");
    result.append("current_weight: ");
    result.append(transactionSignWeight.getCurrentWeight());
    result.append("\n");
    result.append("result:");
    result.append("\n");
    result.append("{");
    result.append("\n");
    result.append(printResult(transactionSignWeight.getResult()));
    result.append("}");
    result.append("\n");
    if (transactionSignWeight.getApprovedListCount() > 0) {
      result.append("approved_list:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (ByteString approved : transactionSignWeight.getApprovedListList()) {
        result.append(TronWallet._encode58Check(approved.toByteArray()));
        result.append("\n");
      }
      result.append("]");
      result.append("\n");
    }
    result.append("transaction:");
    result.append("\n");
    result.append("{");
    result.append("\n");
    result.append(printTransaction(transactionSignWeight.getTransaction()));
    result.append("}");
    result.append("\n");
    return result.toString();
  }

  public static String printTransactionApprovedList(
      TransactionApprovedList transactionApprovedList) {
    StringBuffer result = new StringBuffer();
    result.append("result:");
    result.append("\n");
    result.append("{");
    result.append("\n");
    result.append(printResult(transactionApprovedList.getResult()));
    result.append("}");
    result.append("\n");
    if (transactionApprovedList.getApprovedListCount() > 0) {
      result.append("approved_list:");
      result.append("\n");
      result.append("[");
      result.append("\n");
      for (ByteString approved : transactionApprovedList.getApprovedListList()) {
        result.append(TronWallet._encode58Check(approved.toByteArray()));
        result.append("\n");
      }
      result.append("]");
      result.append("\n");
    }
    result.append("transaction:");
    result.append("\n");
    result.append("{");
    result.append("\n");
    result.append(printTransaction(transactionApprovedList.getTransaction()));
    result.append("}");
    result.append("\n");
    return result.toString();
  }
}

