package edu.uic.cs474.a4;

import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;
import java.util.Random;

import static edu.uic.cs474.a4.Assignment4.*;
import static edu.uic.cs474.a4.Environment.*;
import static edu.uic.cs474.a4.Expression.*;
import static edu.uic.cs474.a4.Value.*;

public class Test10_This {

    @Test
    public void recursiveMethodsShouldWork() {
        Assignment4 a4 = Assignment4.getSolution();

        int c = new Random().nextInt(10) + 1;

        // class Factorializer { int fact(n) { if (n == 1) then 1 else n * this.fact(n-1) } }
        Expression test = new ClassDefExpression(
                new Name("Factorializer"),
                Optional.empty(),
                new Field[]{},
                new Method[]{
                        new Method(
                                new Name("fact"),
                                new Name[]{ new Name("n") },
                                new IfExpression(
                                        new ComparisonExpression(
                                                ComparisonExpression.Type.EQ,
                                                new VariableExpression(new Name("n")),
                                                new IntConstant(1)
                                        ),
                                        new IntConstant(1),
                                        new BinaryOperationExpression(
                                                BinaryOperationExpression.Operator.TIMES,
                                                new VariableExpression(new Name("n")),
                                                new CallMethodExpression(
                                                        new VariableExpression(new Name("this")),
                                                        new Name("fact"),
                                                        new BinaryOperationExpression(
                                                                BinaryOperationExpression.Operator.MINUS,
                                                                new VariableExpression(new Name("n")),
                                                                new IntConstant(1)
                                                        )
                                                )
                                        )
                                )
                        )
                },
                // new Factorializer().fact(c)
                new CallMethodExpression(
                        new NewExpression(new Name("Factorializer")),
                        new Name("fact"),
                        new IntConstant(c)
                )
        );

        // Evaluate the test program
        Value v = a4.evaluate(test, new LexicalScopedEnvironment());

        // And ensure we got the value of the field as the result
        Assert.assertEquals(new IntValue(javaFactorial(c)), v);
    }

    static int javaFactorial(int n) { return (n == 1) ? 1 : n * javaFactorial(n - 1); }
}
