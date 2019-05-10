package io.getty.rntron;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonFormat;
import org.tron.protos.Protocol.Transaction;

import java.util.Arrays;

import static org.tron.protos.Protocol.Transaction.parseFrom;

public class AbstractSignTests {
//    public static Contract.TriggerSmartContract triggerCallContract(byte[] address,
//                                                                    byte[] contractAddress,
//                                                                    long callValue, byte[] data, long tokenValue, String tokenId) {

    public void abstractSign(final String privateKey, final String transaction)
    {
            try {
                String test = "zebra";
                System.out.println(test);

                byte[] ownerPrivateKeyBytes = ByteArray.fromHexString(privateKey);
                ECKey ownerKey = ECKey.fromPrivate(ownerPrivateKeyBytes);

                byte[] transactionBytes = ByteArray.fromHexString(transaction);
                System.out.println(Arrays.toString(transactionBytes));


                Transaction tr = parseFrom(transactionBytes);


                System.out.println(privateKey);
            } catch(Exception e) {
//              System.out.println("Failed to sign transaction");
//              System.out.println(e.getMessage());
            }
    }

    @Test
    public void invoke() throws InvalidProtocolBufferException, JsonFormat.ParseException {
        String privateKey = "5d585f25d609baf62ca1f96687285406a64694f9a5c352c98cf198f3098bbcda";
//        String transaction = "74726f6e626574";
//        String transaction = "0A8F010A022DBE220823653663EFAA2AC940E7BAB382A82D52152053656E742066726F6D2054726F6E57616C6C65745A57080B12530A32747970652E676F6F676C65617069732E636F6D2F70726F746F636F6C2E467265657A6542616C616E6365436F6E7472616374121D0A1541C46D47F7CD8D4E7C91E6A5128441378822971C5D10C0843D18037080BCC180F0FDD4CD15";
//        String transaction = "0a02c985220817502a97bf7b74994088bf96dda72d5ab301081f12ae010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412790a1541c46d47f7cd8d4e7c91e6a5128441378822971c5d1215412ec5f63da00583085d4c2c5e8ec3c8d17bde5e281880ade2042244a3082be90000000000000000000000000000000000000000000000000000000000000009000000000000000000000000000000000000000000000000000000000000000170e58a93dda72d9001809bee02";

//        this.abstractSign(privateKey, transaction);
//        String publicKey = "41c46d47f7cd8d4e7c91e6a5128441378822971c5d";
        String publicKey = "TMzGVjfxjStHtpFsRs6fyawaTCbs1Qryb6";
        String contractAddress = "TKTcfBEKpp5ZRPwmiZ8SfLx8W7CDZ7PHCY";
        String methodStr = "transfer(address,uint256)";

//        triggerCallContract(
//                final String ownerPrivateKey,
//                String ownerAddress,
//                String contractAddress,
//                String callValueStr,
//                String methodStr,
//                String argsStr,
//                String tokenValueStr,
//                String tokenId)

        byte[] contractAddressBytes = TronWallet._decode58Check("TJqV4Qjgv58AwjU7VswnUZSSW5EYDKKrMA");

        String mockTR = "{\n" +
                "  \"txID\": \"bfdeaf18e8d44515a4749a5ebdfeede9d67a396a9284ea582eac287952a4ce67\",\n" +
                "  \"raw_data\": {\n" +
                "    \"contract\": [\n" +
                "      {\n" +
                "        \"parameter\": {\n" +
                "          \"value\": {\n" +
                "            \"data\": \"a3082be900000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000001\",\n" +
                "            \"owner_address\": \"41756bf73fc5d7042aaa4a4b8488befc777a258135\",\n" +
                "            \"contract_address\": \"412ec5f63da00583085d4c2c5e8ec3c8d17bde5e28\",\n" +
                "            \"call_value\": 10000000\n" +
                "          },\n" +
                "          \"type_url\": \"type.googleapis.com/protocol.TriggerSmartContract\"\n" +
                "        },\n" +
                "        \"type\": \"TriggerSmartContract\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"ref_block_bytes\": \"2c37\",\n" +
                "    \"ref_block_hash\": \"f91fb13c67bc4cc0\",\n" +
                "    \"expiration\": 1557523854000,\n" +
                "    \"fee_limit\": 6000000,\n" +
                "    \"timestamp\": 1557523796030\n" +
                "  },\n" +
                "  \"raw_data_hex\": \"0a022c372208f91fb13c67bc4cc040b0e59f9daa2d5ab301081f12ae010a31747970652e676f6f676c65617069732e636f6d2f70726f746f636f6c2e54726967676572536d617274436f6e747261637412790a1541756bf73fc5d7042aaa4a4b8488befc777a2581351215412ec5f63da00583085d4c2c5e8ec3c8d17bde5e281880ade2042244a3082be90000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000170bea09c9daa2d9001809bee02\",\n" +
                "}";
        JSONObject res = TronWallet.buildTriggerSmartContract2(
            privateKey,
            mockTR
        );

        System.out.println(res.toJSONString());

//        }
        //TODO Need method to handle
        // Check methodStr, params here a splited by comma
//        String methodParamsStr = "\"" + TronWallet._encode58Check(contractAddressBytes) + "\",\"1000000\"";
//
//        JSONObject res = TronWallet.buildTriggerSmartContract(
//            privateKey,
//            contractAddress,
//            "0",
//            methodStr,
//            "",
//            "0",
//            "0"
//        );

//        System.out.println(res.toJSONString());
//
//        String address = TronWallet.addressFromPk(privateKey);
//
//        String[] keypair = TronWallet.generateKeypair("color word gorilla elder leopard impulse link blame fabric shadow mobile embark", 0, false);
//
//        System.out.println("address: "+keypair[0]);
//        System.out.println("privateKey: "+keypair[1]);
//        System.out.println("publicKey: "+keypair[2]);
//        System.out.println("password: "+keypair[3]);
//
//        String mnemonic = TronWallet.generateMnemonic();
//        System.out.println("mnemonic: "+mnemonic);

//        RNTronModule.triggerCallContract( address,
//                 contractAddress,
//                 callValueStr,  data,  tokenValueStr,  tokenId)

//        byte[] transactionBytes = ByteArray.fromHexString(hexContract);
//        System.out.println(hexContract);

    }
}