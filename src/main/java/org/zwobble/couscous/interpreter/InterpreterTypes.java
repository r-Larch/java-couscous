package org.zwobble.couscous.interpreter;

import org.zwobble.couscous.ast.TypeName;
import org.zwobble.couscous.interpreter.values.InterpreterValue;

public class InterpreterTypes {
    public static void checkIsInstance(TypeName type, InterpreterValue value) {
        checkIsSubType(type, value.getType().getName());
    }

    private static void checkIsSubType(TypeName superType, TypeName subType) {
        if (!superType.equals(subType)) {
            throw new UnexpectedValueType(superType, subType);
        }
    }
}