package com.gts.ysmu.geckolib3.core.molang.binding.variable;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.geckolib3.core.molang.storage.IControllerVariableStorage;
import com.gts.ysmu.geckolib3.core.molang.util.StringPool;
import com.gts.ysmu.geckolib3.core.molang.binding.ResetVariable;
import com.gts.ysmu.molang.runtime.AssignableVariable;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.binding.ObjectBinding;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("MapOrSetKeyShouldOverrideHashCodeEquals")
public class ControllerVariableBinding implements ObjectBinding, ResetVariable {

    private final Int2ReferenceOpenHashMap<ControllerVariable> variableMap = new Int2ReferenceOpenHashMap<>();

    @Override
    public Object getProperty(String name) {
        return variableMap.computeIfAbsent(StringPool.computeIfAbsent(name), ControllerVariable::new);
    }

    public void reset() {
        variableMap.clear();
    }

    private record ControllerVariable(int name) implements AssignableVariable {
        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            IControllerVariableStorage storage = ((IContext<Object>) context.entity()).controllerStorage();
            if (storage != null) {
                return storage.getControllerVariable(this.name);
            }
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
            IControllerVariableStorage storage = ((IContext<Object>) context.entity()).controllerStorage();
            if (storage != null) {
                storage.setControllerVariable(this.name, value);
            }
        }
    }
}
