package software.bernie.geckolib3.watchers;

import software.bernie.geckolib3.particles.BedrockLibrary;

import java.io.File;

public class ParticleDirWatcher extends AbstractDirWatcher {
    public ParticleDirWatcher(File file) {
        super(file.toPath(), true);
    }

    @Override
    public void processCreate(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        BedrockLibrary.instance.storeScheme(path);
    }

    @Override
    public void processDelete(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        BedrockLibrary.instance.remove(name);
    }

    @Override
    public void processModify(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        BedrockLibrary.instance.storeScheme(path);
    }
}
