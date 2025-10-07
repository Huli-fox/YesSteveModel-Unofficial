package software.bernie.geckolib3.watchers;

import software.bernie.geckolib3.resource.AnimationLibrary;

import java.io.File;

public class AnimationDirWatcher extends AbstractDirWatcher {
    public AnimationDirWatcher(File file) {
        super(file.toPath(), true);
    }

    @Override
    public void processCreate(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        AnimationLibrary.instance.storeModel(path);
    }

    @Override
    public void processDelete(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        AnimationLibrary.instance.remove(path);
    }

    @Override
    public void processModify(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        AnimationLibrary.instance.storeModel(path);
    }
}
