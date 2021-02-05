package edu.uic.cs474.a4;

import edu.uic.cs474.a4.*;

import java.util.ArrayList;
import java.util.Optional;

import static edu.uic.cs474.a4.Assignment4.*;
import static edu.uic.cs474.a4.Environment.*;
import static edu.uic.cs474.a4.Expression.*;
import static edu.uic.cs474.a4.Value.*;
import static edu.uic.cs474.a4.Interpreter.*;

public class A4Solution extends Assignment4 {
    private Name recentMethodCallClass;

    @Override
    public Value evaluate(Expression c, Environment e) {
        switch(c.getClass().getSimpleName()) {
            case "IntConstant": {
                IntConstant cst = (IntConstant) c;
                IntValue ret = new IntValue(cst.c);
                return ret;
            }
            case "VariableExpression": {
                VariableExpression var = (VariableExpression) c;
                return e.lookup(var.variable);
            }
            case "InstanceOfExpression": {
                InstanceOfExpression ioe = (InstanceOfExpression) c;

                Value target = evaluate(ioe.target, e);

                if(target instanceof ObjectValue) {
                    ObjectValue obj = (ObjectValue) target;
                    ClassValue t = (ClassValue) e.lookup(obj.name);
                    final boolean equals = obj.name.toString().equals(ioe.className.toString());
                    if(t.superName.isEmpty()) {
                        if(equals)
                            return new BooleanValue(true);
                    }
                    else {
                        Name Super = t.superName.get();
                        while(!t.superName.isEmpty()){
                            ClassValue temp = (ClassValue) e.lookup(Super);
                            if(equals || Super.equals(ioe.className))
                                return new BooleanValue(true);
                            if(temp.superName.isEmpty())
                                return new BooleanValue(false);
                            Super = temp.superName.get();
                        }
                    }
                    return new BooleanValue(false);
                }
                return new BooleanValue(false);
            }
            case "ClassDefExpression": {
                ClassDefExpression def = (ClassDefExpression) c;
                ClassValue ret = new ClassValue(def.className, def.superName, def.methods, def.body, def.fields);

                return evaluate(def.body, e.bind(def.className, ret));
            }
            case "NewExpression": {
                NewExpression ne = (NewExpression) c;
                ClassValue cl = (ClassValue) e.lookup(ne.className);
                ObjectValue ret;
                if (cl.fields.length != 0) {
                    ret = new ObjectValue(ne.className.toString(), cl.fields);
                }
                else {
                    ret = new ObjectValue(ne.className.toString(), (Field[]) null);
                }
                return ret;
            }
            case "ReadFieldExpression": {
                ReadFieldExpression rfe = (ReadFieldExpression) c;
                ObjectValue t = (ObjectValue) evaluate(rfe.receiver,e);
                Expression expression;
                Value value = t;
                ClassValue classValue = (ClassValue) e.lookup(t.name);
                if(classValue.superName.isPresent()) {
                    ClassValue temp = (ClassValue) e.lookup(classValue.className);
                    do {
                        temp = (ClassValue) e.lookup(classValue.superName.get());
                        for(int i = 0; i < temp.fields.length; i++) {
                            if(temp.fields[i].name.toString().equals(rfe.fieldName.toString())) {
                                expression = temp.fields[i].initializer;
                                return evaluate(expression, e);
                            }
                        }
                    }
                    while(temp.superName.isPresent());
                }
                for(int i = 0 ; i < t.fields.length; i++) {
                    if(t.fields[i].name.toString().equals(rfe.fieldName.toString())) {
                        expression = t.fields[i].initializer;
                        value = evaluate(expression, e);
                    }
                }

                return value;
            }
            case "WriteFieldExpression": {

                throw new Error("Not implemented 5");

            }
            case "CallMethodExpression": {
                //Get the obj
                CallMethodExpression cme = (CallMethodExpression) c;
                ClassValue classObj;
                ObjectValue obj = null;
                Method m = null;
                if(cme.receiver instanceof VariableExpression){
                    VariableExpression expression = (VariableExpression) cme.receiver;
                    if(expression.variable.theName.equals("this")) {
                        classObj = (ClassValue) e.lookup(recentMethodCallClass);
                        for (int i = 0; i < classObj.methods.length; i++) {
                            if (cme.methodName.equals(classObj.methods[i].name)) {
                                m = classObj.methods[i];
                            }
                        }
                        return getValue(e, cme, m);
                    }
                    obj = (ObjectValue) evaluate(cme.receiver, e);
                }
                else {
                    //recentMethodCallClass = cme.
                    obj = (ObjectValue) evaluate(cme.receiver, e);
                }
                Expression expression = cme.receiver;
                classObj = (ClassValue) e.lookup(obj.name);
                recentMethodCallClass = classObj.className;
                if(classObj.superName.isEmpty()) {
                    for (int i = 0; i < classObj.methods.length; i++) {
                        if (cme.methodName.equals(classObj.methods[i].name)) {
                            m = classObj.methods[i];
                        }
                    }
                }
                else {
                    ClassValue superclass = (ClassValue) e.lookup(classObj.superName.get());
                    ClassValue temp = classObj;
                    do {
                        if(superclass.superName.isPresent())
                            superclass = (ClassValue) e.lookup(superclass.superName.get());
                        for (int i = 0; i < classObj.methods.length; i++) {
                            if (cme.methodName.equals(classObj.methods[i].name)) {
                                m = classObj.methods[i];
                                return getValue(e, cme, m);
                            }
                        }
                        temp = classObj;
                        classObj = superclass;
                    }
                    while(temp.superName.isPresent());
                }
                return getValue(e, cme, m);
                // Get the method
                // Consider the hierarchy

                // Call the method
                // Calling functions in Lecture 17
                // Ensure that this works for test 10

            }
            default: {
                return Interpreter.evaluate(c, e,this);
            }
        }
    }

    private Value getValue(Environment e, CallMethodExpression cme, Method m) {
        for(int i = 0; i < cme.arguments.length; i++) {
            Value value = evaluate(cme.arguments[i], e);
            e = e.bind(m.arguments[i], value);
        }
        Value value = evaluate(m.body, e);
        return value;
    }

    public static class ClassValue extends Value {
        // keep information here from new
        public final Name className;
        public final Optional<Name> superName;
        public final Field[] fields;
        public final Method[] methods;
        public final Expression body;
        public final int counter;


        public ClassValue(Name className, Optional<Name> superName, Method[] methods, Expression body, Field ... fields) {
            this.className = className;
            this.superName = superName;
            this.fields = fields;
            this.methods = methods;
            this.body = body;
            this.counter = 0;
        }
    }

    public static class ObjectValue extends Value {
        final Name name;
        final Field[] fields;

        //keeps information here from new
        public ObjectValue(String toString, Field ... fields) {
            this.name = new Name(toString);
            this.fields = fields;
        }

    }

}
