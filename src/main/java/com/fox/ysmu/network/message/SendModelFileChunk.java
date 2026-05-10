package com.fox.ysmu.network.message;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.client.ClientModelManager;
import com.fox.ysmu.model.ServerModelManager;
import com.fox.ysmu.ysmu;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

public class SendModelFileChunk implements IMessage {

    public static final int MAX_CHUNK_BYTES = 512 * 1024;

    private static final Map<String, ChunkAccumulator> ACCUMULATORS = new ConcurrentHashMap<>();

    private String fileName;
    private int totalLength;
    private int offset;
    private byte[] data = new byte[0];

    public SendModelFileChunk() {}

    public SendModelFileChunk(String fileName, int totalLength, int offset, byte[] data) {
        this.fileName = fileName;
        this.totalLength = totalLength;
        this.offset = offset;
        this.data = data == null ? new byte[0] : data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.fileName = ByteBufUtils.readUTF8String(buf);
        this.totalLength = buf.readInt();
        this.offset = buf.readInt();
        int length = buf.readInt();
        validateLengths(this.totalLength, this.offset, length);
        this.data = new byte[length];
        buf.readBytes(this.data);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        byte[] safeData = this.data == null ? new byte[0] : this.data;
        validateLengths(this.totalLength, this.offset, safeData.length);
        ByteBufUtils.writeUTF8String(buf, this.fileName);
        buf.writeInt(this.totalLength);
        buf.writeInt(this.offset);
        buf.writeInt(safeData.length);
        buf.writeBytes(safeData);
    }

    private static void validateLengths(int totalLength, int offset, int length) {
        if (totalLength < 0 || offset < 0 || length < 0 || length > MAX_CHUNK_BYTES
            || offset > totalLength - length) {
            throw new IllegalArgumentException(
                "Invalid YSM model chunk: total=" + totalLength + ", offset=" + offset + ", length=" + length);
        }
    }

    public static class Handler implements IMessageHandler<SendModelFileChunk, IMessage> {

        @Override
        public IMessage onMessage(SendModelFileChunk message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                receiveChunk(message);
            }
            return null;
        }

        private void receiveChunk(SendModelFileChunk message) {
            if (!isValidCacheFileName(message.fileName)) {
                ysmu.LOG.warn("Ignoring YSM model chunk with invalid cache file name {}", message.fileName);
                return;
            }

            File cacheDir = ServerModelManager.CACHE_CLIENT.toFile();
            if (!cacheDir.isDirectory() && !cacheDir.mkdirs()) {
                ysmu.LOG.warn("Failed to create YSM client model cache directory: {}", cacheDir);
                return;
            }

            File partFile = ServerModelManager.CACHE_CLIENT.resolve(message.fileName + ".part")
                .toFile();
            File outputFile = ServerModelManager.CACHE_CLIENT.resolve(message.fileName)
                .toFile();
            ChunkAccumulator accumulator = ACCUMULATORS.compute(
                message.fileName,
                (name, existing) -> existing != null && existing.totalLength == message.totalLength
                    ? existing
                    : new ChunkAccumulator(message.totalLength));

            boolean complete;
            synchronized (accumulator) {
                try {
                    writeChunk(partFile, message);
                } catch (IOException e) {
                    ACCUMULATORS.remove(message.fileName, accumulator);
                    ysmu.LOG.warn("Failed to write YSM model chunk for " + message.fileName, e);
                    return;
                }
                complete = accumulator.addChunk(message.offset, message.data.length);
            }

            if (complete) {
                finishChunkedFile(message.fileName, partFile, outputFile, accumulator);
            }
        }

        private void writeChunk(File partFile, SendModelFileChunk message) throws IOException {
            try (RandomAccessFile file = new RandomAccessFile(partFile, "rw")) {
                if (message.offset == 0 && file.length() != message.totalLength) {
                    file.setLength(message.totalLength);
                }
                file.seek(message.offset);
                file.write(message.data);
            }
        }

        private void finishChunkedFile(String fileName, File partFile, File outputFile, ChunkAccumulator accumulator) {
            synchronized (accumulator) {
                try {
                    FileUtils.deleteQuietly(outputFile);
                    FileUtils.moveFile(partFile, outputFile);
                    ACCUMULATORS.remove(fileName, accumulator);
                    ClientModelManager.rememberCachedModel(fileName);
                    RequestLoadModel.loadModel(fileName);
                    ysmu.LOG.info(
                        "Received chunked YSM model cache file {} ({} bytes)",
                        fileName,
                        accumulator.totalLength);
                } catch (IOException e) {
                    ACCUMULATORS.remove(fileName, accumulator);
                    ysmu.LOG.warn("Failed to finish YSM chunked model cache file " + fileName, e);
                }
            }
        }

        private boolean isValidCacheFileName(String fileName) {
            if (fileName == null || fileName.length() != 32) {
                return false;
            }
            for (int i = 0; i < fileName.length(); i++) {
                char c = fileName.charAt(i);
                boolean hex = c >= '0' && c <= '9' || c >= 'a' && c <= 'f' || c >= 'A' && c <= 'F';
                if (!hex) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ChunkAccumulator {

        private final int totalLength;
        private final Set<Integer> offsets = new HashSet<>();
        private int receivedLength;

        private ChunkAccumulator(int totalLength) {
            this.totalLength = totalLength;
        }

        private boolean addChunk(int offset, int length) {
            if (this.offsets.add(offset)) {
                this.receivedLength += length;
            }
            return this.receivedLength >= this.totalLength;
        }
    }
}
