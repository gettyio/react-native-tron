package io.getty.rntron;

import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.tron.common.utils.JsonFormat;

import wallet.core.jni.CoinType;
import wallet.core.jni.Curve;
import wallet.core.jni.HDVersion;
import wallet.core.jni.HDWallet;
import wallet.core.jni.PrivateKey;
import wallet.core.jni.Purpose;

import wallet.core.jni.proto.Bitcoin;

public class AbstractSignTests {

    @Test
    public void invoke() throws InvalidProtocolBufferException, JsonFormat.ParseException {

        HDWallet wallet = new HDWallet("ripple scissors kick mammal hire column oak again sun offer wealth tomorrow", null);
        PrivateKey key0 = wallet.getKeyBIP44(CoinType.BITCOIN, 0,0,0);
    }
}
