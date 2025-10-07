//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.eliotlash.molang.expressions;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.eliotlash.molang.MolangParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import software.bernie.geckolib3.molang.MolangRegistrar;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MolangValue extends MolangExpression {
    public IValue value;
    public boolean returns;

    public MolangValue() {
    }

    public MolangValue(MolangParser context, IValue value) {
        super(context);
        this.value = value;
    }

    public MolangExpression addReturn() {
        this.returns = true;
        return this;
    }

    public double get() {
        return this.value.get();
    }

    public String toString() {
        return (this.returns ? "return " : "") + this.value.toString();
    }

    public JsonElement toJson() {
        return (JsonElement) (this.value instanceof Constant ? new JsonPrimitive(this.value.get()) : super.toJson());
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(value);
        out.writeBoolean(returns);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        value = (IValue) in.readObject();
        returns = in.readBoolean();
        context = MolangRegistrar.getParser();
    }
}
