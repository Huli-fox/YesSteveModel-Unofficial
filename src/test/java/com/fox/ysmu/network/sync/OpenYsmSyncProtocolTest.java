package com.fox.ysmu.network.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.fox.ysmu.network.message.C2SCompleteFeedback17;
import com.fox.ysmu.network.message.C2SModelSyncPayload17;
import com.fox.ysmu.network.message.C2SVersionCheck17;
import com.fox.ysmu.network.message.S2CModelSyncPayload17;
import com.fox.ysmu.network.message.S2CVersionCheck17;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class OpenYsmSyncProtocolTest {

    @Test
    void modelSyncPayloadMessagesRoundTripOpaqueBytes() {
        byte[] payload = new byte[] { 1, 2, 3, 4, 5 };

        C2SModelSyncPayload17 c2s = new C2SModelSyncPayload17(payload);
        ByteBuf c2sBuf = Unpooled.buffer();
        c2s.toBytes(c2sBuf);
        C2SModelSyncPayload17 decodedC2s = new C2SModelSyncPayload17();
        decodedC2s.fromBytes(c2sBuf);

        S2CModelSyncPayload17 s2c = new S2CModelSyncPayload17(payload);
        ByteBuf s2cBuf = Unpooled.buffer();
        s2c.toBytes(s2cBuf);
        S2CModelSyncPayload17 decodedS2c = new S2CModelSyncPayload17();
        decodedS2c.fromBytes(s2cBuf);

        assertArrayEquals(payload, decodedC2s.getData());
        assertArrayEquals(payload, decodedS2c.getData());
    }

    @Test
    void versionAndFeedbackMessagesRoundTrip() {
        C2SVersionCheck17 c2sVersion = new C2SVersionCheck17("client-version");
        ByteBuf c2sVersionBuf = Unpooled.buffer();
        c2sVersion.toBytes(c2sVersionBuf);
        C2SVersionCheck17 decodedC2sVersion = new C2SVersionCheck17();
        decodedC2sVersion.fromBytes(c2sVersionBuf);

        S2CVersionCheck17 s2cVersion = new S2CVersionCheck17("server-version");
        ByteBuf s2cVersionBuf = Unpooled.buffer();
        s2cVersion.toBytes(s2cVersionBuf);
        S2CVersionCheck17 decodedS2cVersion = new S2CVersionCheck17();
        decodedS2cVersion.fromBytes(s2cVersionBuf);

        C2SCompleteFeedback17 feedback = new C2SCompleteFeedback17(
            C2SCompleteFeedback17.STATUS_SUCCESS,
            3,
            2,
            1,
            "done");
        ByteBuf feedbackBuf = Unpooled.buffer();
        feedback.toBytes(feedbackBuf);
        C2SCompleteFeedback17 decodedFeedback = new C2SCompleteFeedback17();
        decodedFeedback.fromBytes(feedbackBuf);

        assertEquals("client-version", decodedC2sVersion.getVersion());
        assertEquals("server-version", decodedS2cVersion.getVersion());
        assertEquals(C2SCompleteFeedback17.STATUS_SUCCESS, decodedFeedback.getStatus());
        assertEquals(3, decodedFeedback.getLoaded());
        assertEquals(2, decodedFeedback.getDownloaded());
        assertEquals(1, decodedFeedback.getCacheHits());
        assertEquals("done", decodedFeedback.getMessage());
    }

    @Test
    void clientCacheKeyIsStableAndDistinctFromServerKey() {
        byte[] serverKey = new byte[56];
        for (int i = 0; i < serverKey.length; i++) {
            serverKey[i] = (byte) i;
        }

        byte[] first = OpenYsmModelSyncServer.createClientCacheKey(serverKey);
        byte[] second = OpenYsmModelSyncServer.createClientCacheKey(serverKey);

        assertArrayEquals(first, second);
        assertFalse(java.util.Arrays.equals(serverKey, first));
    }
}
