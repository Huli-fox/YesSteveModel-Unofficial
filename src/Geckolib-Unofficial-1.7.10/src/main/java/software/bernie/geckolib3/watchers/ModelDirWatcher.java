package software.bernie.geckolib3.watchers;

import software.bernie.geckolib3.resource.ModelLibrary;

import java.io.File;

public class ModelDirWatcher extends AbstractDirWatcher {
    public ModelDirWatcher(File file) {
        super(file.toPath(), true);
    }

    @Override
    public void processCreate(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        ModelLibrary.instance.storeModel(path);
    }

    @Override
    public void processDelete(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        ModelLibrary.instance.remove(path);
    }

    @Override
    public void processModify(File path) {
        String name = path.getName();
        if (!name.endsWith(".json")) return;
        ModelLibrary.instance.storeModel(path);
    }
}
