package com.fox.ysmu.model.format;

import static com.fox.ysmu.model.ServerModelManager.CACHE_SERVER;

import java.util.Locale;

import org.apache.commons.io.FileUtils;

import com.fox.ysmu.data.EncryptTools;
import com.fox.ysmu.data.ModelData;
import com.fox.ysmu.util.Md5Utils;

final class ModelCacheWriter {

    private ModelCacheWriter() {}

    static ServerModelInfo write(ModelData data) throws Exception {
        byte[] dataBytes = EncryptTools.assembleEncryptModels(data);
        data.setMd5(Md5Utils.md5Hex(dataBytes).toUpperCase(Locale.US));
        FileUtils.writeByteArrayToFile(
            CACHE_SERVER.resolve(
                data.getInfo()
                    .getMd5())
                .toFile(),
            dataBytes);
        return data.getInfo();
    }
}
