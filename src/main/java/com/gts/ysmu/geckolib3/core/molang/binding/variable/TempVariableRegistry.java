package com.gts.ysmu.geckolib3.core.molang.binding.variable;

import com.gts.ysmu.geckolib3.core.molang.context.IContext;
import com.gts.ysmu.molang.runtime.AssignableVariable;
import com.gts.ysmu.molang.runtime.ExecutionContext;
import com.gts.ysmu.molang.runtime.binding.ObjectBinding;
import com.gts.ysmu.geckolib3.core.molang.binding.CloseVariable;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;

public class TempVariableRegistry implements ObjectBinding, CloseVariable {

    private final Object2ReferenceMap<String, TempVariable> variableMap = new Object2ReferenceOpenHashMap();

    private int topPointer = 0;

    @Override
    public Object getProperty(String str) {
        return this.variableMap.computeIfAbsent(str, obj -> {
            int i = this.topPointer;
            this.topPointer = i + 1;
            return new TempVariable(i);
        });
    }

    @Override
    public void dispose() {
        this.variableMap.clear();
        this.topPointer = 0;
    }

    private record TempVariable(int address) implements AssignableVariable {
        @Override
        @SuppressWarnings("unchecked")
        public Object evaluate(@NotNull ExecutionContext<?> context) {
            return ((IContext<Object>) context.entity()).tempStorage().getElement(this.address);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void assign(@NotNull ExecutionContext<?> context, Object value) {
            ((IContext<Object>) context.entity()).tempStorage().setElement(this.address, value);
        }
    }
}
