//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.eliotlash.molang.expressions;

import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.Variable;
import com.eliotlash.molang.MolangParser;
import software.bernie.geckolib3.molang.MolangRegistrar;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class MolangAssignment extends MolangExpression {
    public Variable variable;
    public IValue expression;

    public MolangAssignment() {
    }

    public MolangAssignment(MolangParser context, Variable variable, IValue expression) {
        super(context);
        this.variable = variable;
        this.expression = expression;
    }

    public double get() {
        double value = this.expression.get();
        this.variable.set(value);
        return value;
    }

    public String toString() {
        return this.variable.getName() + " = " + this.expression.toString();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(variable);
        out.writeObject(expression);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        variable = (Variable) in.readObject();
        expression = (MolangExpression) in.readObject();
        context = MolangRegistrar.getParser();
    }
}
