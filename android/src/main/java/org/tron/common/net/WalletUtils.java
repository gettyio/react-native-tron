//package org.tron.common.net;
//
//import org.tron.common.utils.StringUtils;
//import org.tron.common.crypto.Hash;
//import org.tron.common.crypto.SymmEncoder;
//import org.tron.common.utils.ByteArray;
//import org.tron.common.utils.Sha256Hash;
//
//import org.tron.protos.Protocol;
//
//
//public class WalletUtils {
//
//    public static void saveWallet(Wallet wallet, String password) {
//
//        if (!wallet.isWatchOnly()) {
//
//            byte[] pwd = StringUtils.getPasswordHash(password);
//            String pwdAsc = ByteArray.toHexString(pwd);
//
//            //encrypted by password
//            byte[] aseKey = StringUtils.getEncKey(password);
//
//            byte[] privKeyPlain = wallet.getECKey().getPrivKeyBytes();
//            byte[] privKeyEnced = SymmEncoder.AES128EcbEnc(privKeyPlain, aseKey);
//            String privKeyStr = ByteArray.toHexString(privKeyEnced);
//            String mnemonicStr = null;
//            if (!StringUtils.isEmpty(wallet.getMnemonic())) {
//                byte[] mnemonicPlain = wallet.getMnemonic().getBytes();
//                byte[] mnemonicEnced = SymmEncoder.AESEcbEnc(mnemonicPlain, aseKey);
//                mnemonicStr = ByteArray.toHexString(mnemonicEnced);
//            }
//
//
//            byte[] pubKeyBytes = wallet.getECKey().getPubKey();
//            String pubKeyStr = ByteArray.toHexString(pubKeyBytes);
//        }
//    }
//
//
//
//
//
//
//
//    public static byte[] generateContractAddress(Protocol.Transaction trx, byte[] ownerAddress) {
//        // get tx hash
//        byte[] txRawDataHash = Sha256Hash.of(trx.getRawData().toByteArray()).getBytes();
//
//        // combine
//        byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
//        System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
//        System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);
//
//        return Hash.sha3omit12(combined);
//    }
//}
