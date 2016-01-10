package org.zwobble.couscous.ast;

import org.zwobble.couscous.values.BooleanValue;
import org.zwobble.couscous.values.IntegerValue;

import java.util.Collections;

import static org.zwobble.couscous.ast.MethodCallNode.methodCall;
import static org.zwobble.couscous.util.ExtraLists.list;

public class Operations {
    public static ExpressionNode not(ExpressionNode value) {
        if (value.getType().equals(BooleanValue.REF)) {
            return methodCall(value, "negate", Collections.emptyList(), BooleanValue.REF);
        } else {
            throw new IllegalArgumentException("Can only negate booleans");
        }
    }

    public static ExpressionNode booleanAnd(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.BOOLEAN_AND.getMethodName(), left, right);
    }

    public static ExpressionNode booleanOr(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.BOOLEAN_OR.getMethodName(), left, right);
    }

    public static ExpressionNode integerAdd(ExpressionNode left, ExpressionNode right) {
        return integerOperation(Operator.ADD.getMethodName(), left, right);
    }

    public static ExpressionNode integerSubtract(ExpressionNode left, ExpressionNode right) {
        return integerOperation(Operator.SUBTRACT.getMethodName(), left, right);
    }

    public static ExpressionNode integerMultiply(ExpressionNode left, ExpressionNode right) {
        return integerOperation(Operator.MULTIPLY.getMethodName(), left, right);
    }

    public static ExpressionNode integerDivide(ExpressionNode left, ExpressionNode right) {
        return integerOperation(Operator.DIVIDE.getMethodName(), left, right);
    }

    public static ExpressionNode integerMod(ExpressionNode left, ExpressionNode right) {
        return integerOperation(Operator.MOD.getMethodName(), left, right);
    }

    public static MethodCallNode integerOperation(String methodName, ExpressionNode left, ExpressionNode right) {
        return methodCall(left, methodName, list(right), IntegerValue.REF);
    }

    public static ExpressionNode equal(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.EQUALS.getMethodName(), left, right);
    }

    public static ExpressionNode notEqual(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.NOT_EQUALS.getMethodName(), left, right);
    }

    public static ExpressionNode greaterThan(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.GREATER_THAN.getMethodName(), left, right);
    }

    public static ExpressionNode greaterThanOrEqual(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.GREATER_THAN_OR_EQUAL.getMethodName(), left, right);
    }

    public static ExpressionNode lessThan(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.LESS_THAN.getMethodName(), left, right);
    }

    public static ExpressionNode lessThanOrEqual(ExpressionNode left, ExpressionNode right) {
        return booleanOperation(Operator.LESS_THAN_OR_EQUAL.getMethodName(), left, right);
    }

    private static ExpressionNode booleanOperation(String methodName, ExpressionNode left, ExpressionNode right) {
        return methodCall(left, methodName, list(right), BooleanValue.REF);
    }
}
