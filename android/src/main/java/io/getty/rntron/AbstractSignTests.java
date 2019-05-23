package io.getty.rntron;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.junit.Test;
import org.tron.common.crypto.ECKey;
import org.tron.common.utils.ByteArray;
import org.tron.common.utils.JsonFormat;
import org.tron.common.utils.Utils;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Transaction;

import static org.tron.protos.Protocol.Transaction.parseFrom;

public class AbstractSignTests {

    @Test
    public void invoke() throws InvalidProtocolBufferException, JsonFormat.ParseException {
    }
}
