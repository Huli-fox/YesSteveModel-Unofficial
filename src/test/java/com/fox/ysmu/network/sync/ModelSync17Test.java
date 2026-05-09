package com.fox.ysmu.network.sync;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import rip.ysm.security.YSMByteBuf;
import rip.ysm.security.YsmCrypt;

class ModelSync17Test {

    @Test
    void modelHashKeepsServerCacheFileNameAndUuidStable() {
        ModelHash17 hash = new ModelHash17(0x0123456789ABCDEFL, 0x0FEDCBA987654321L);

        assertEquals("0123456789abcdef0fedcba987654321", hash.toServerCacheFileName());
        assertEquals(0x0123456789ABCDEFL, hash.asUuid().getMostSignificantBits());
        assertEquals(0x0FEDCBA987654321L, hash.asUuid().getLeastSignificantBits());
        assertEquals(hash, new ModelHash17(0x0123456789ABCDEFL, 0x0FEDCBA987654321L));
        assertNotEquals(hash, new ModelHash17(1L, 2L));
    }

    @Test
    void sync17Packet01AndPacket02HandshakeRoundTrip() throws Exception {
        byte[] packet01;
        byte[] key1;
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeGarbageHeader(3, new byte[] { 1, 2, 3 });
            out.writeByte((byte) 0x01);
            YsmCrypt.EncryptedPacket encrypted = YsmCrypt.encrypt(out.toArray(), YsmCrypt.publicKey, true);
            packet01 = encrypted.data();
            key1 = encrypted.nextKey();
        }

        byte[] decrypted01 = YsmCrypt.decrypt(packet01, YsmCrypt.publicKey);
        assertNotNull(decrypted01);
        assertArrayEquals(key1, java.util.Arrays.copyOfRange(decrypted01, decrypted01.length - 56, decrypted01.length));

        byte[] packet02 = ModelSyncClient17.buildPacket02(key1);
        byte[] decrypted02 = YsmCrypt.decrypt(packet02, key1);
        assertEquals(0x02, readPacketTypeBeforeNextKey(decrypted02));
        assertEquals(56, decrypted02.length - payloadLengthBeforeNextKey(decrypted02));
    }

    @Test
    void sync17Packet04MissListRoundTripsThroughEncryption() throws Exception {
        byte[] key = "01234567890123456789012345678901012345678901234567890123".getBytes(StandardCharsets.UTF_8);
        ModelHash17 first = new ModelHash17(11L, 22L);
        ModelHash17 second = new ModelHash17(33L, 44L);

        byte[] packet;
        try (YSMByteBuf out = new YSMByteBuf(Unpooled.buffer())) {
            out.writeGarbageHeader(2, new byte[] { 9, 8 });
            out.writeByte((byte) 0x04);
            out.writeVarInt(2);
            out.writeVarLong(first.getHash1());
            out.writeVarLong(first.getHash2());
            out.writeVarLong(second.getHash1());
            out.writeVarLong(second.getHash2());
            packet = YsmCrypt.encrypt(out.toArray(), key, false).data();
        }

        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(YsmCrypt.decrypt(packet, key)))) {
            in.skipGarbageHeader();
            assertEquals(0x04, in.readByte());
            assertEquals(2, in.readVarInt());
            assertEquals(first.getHash1(), in.readVarLong());
            assertEquals(first.getHash2(), in.readVarLong());
            assertEquals(second.getHash1(), in.readVarLong());
            assertEquals(second.getHash2(), in.readVarLong());
        }
    }

    private static int readPacketTypeBeforeNextKey(byte[] decrypted) {
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
            in.skipGarbageHeader();
            return in.readByte();
        }
    }

    private static int payloadLengthBeforeNextKey(byte[] decrypted) {
        try (YSMByteBuf in = new YSMByteBuf(Unpooled.wrappedBuffer(decrypted))) {
            in.skipGarbageHeader();
            in.readByte();
            in.readByte();
            return in.getRawBuf().readerIndex();
        }
    }
}
