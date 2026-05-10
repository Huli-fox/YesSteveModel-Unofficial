package software.bernie.geckolib3.core.molang.functions;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.functions.Function;
import com.fox.ysmu.client.animation.molang.MolangPhysicsRuntime;

public class BonePosition extends Function {

    public BonePosition(IValue[] values, String name) throws Exception {
        super(values, name);
    }

    @Override
    public int getRequiredArguments() {
        return 1;
    }

    @Override
    public double get() {
        return MolangPhysicsRuntime.bonePosition((int) getArg(0), axis());
    }

    private char axis() {
        return this.name == null || this.name.isEmpty() ? 'x' : this.name.charAt(this.name.length() - 1);
    }
}
