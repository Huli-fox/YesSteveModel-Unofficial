//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.eliotlash.molang.expressions;

import com.eliotlash.mclib.math.Variable;
import com.eliotlash.molang.MolangParser;
import software.bernie.geckolib3.molang.MolangRegistrar;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class MolangMultiStatement extends MolangExpression {
    public List<MolangExpression> expressions = new ArrayList();
    public Map<String, Variable> locals = new HashMap();

    public MolangMultiStatement() {
        super();
    }

    public MolangMultiStatement(MolangParser context) {
        super(context);
    }

    public double get() {
        double value = 0.0;

        MolangExpression expression;
        for (Iterator var3 = this.expressions.iterator(); var3.hasNext(); value = expression.get()) {
            expression = (MolangExpression) var3.next();
        }

        return value;
    }

    public String toString() {
        StringJoiner builder = new StringJoiner("; ");
        Iterator var2 = this.expressions.iterator();

        while (var2.hasNext()) {
            MolangExpression expression = (MolangExpression) var2.next();
            builder.add(expression.toString());
            if (expression instanceof MolangValue && ((MolangValue) expression).returns) {
                break;
            }
        }

        return builder.toString();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(expressions);
        out.writeObject(locals);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        expressions = (List<MolangExpression>) in.readObject();
        locals = (Map<String, Variable>) in.readObject();
        context = MolangRegistrar.getParser();
    }
}
