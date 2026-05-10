package software.bernie.geckolib3.core.molang;

import java.util.function.DoubleSupplier;

import com.fox.ysmu.client.animation.molang.MolangPhysicsRuntime;

public class ScopedMolangVariable extends LazyVariable {

    private DoubleSupplier fallbackSupplier;

    public ScopedMolangVariable(String name, double value) {
        this(name, () -> value);
    }

    public ScopedMolangVariable(String name, DoubleSupplier fallbackSupplier) {
        super(name, fallbackSupplier);
        this.fallbackSupplier = fallbackSupplier;
    }

    @Override
    public void set(double value) {
        if (!MolangPhysicsRuntime.setVariable(getName(), value)) {
            this.fallbackSupplier = () -> value;
            super.set(value);
        }
    }

    @Override
    public void set(DoubleSupplier valueSupplier) {
        this.fallbackSupplier = valueSupplier;
        super.set(valueSupplier);
    }

    @Override
    public double get() {
        return MolangPhysicsRuntime.getVariable(getName(), fallbackSupplier.getAsDouble());
    }
}
