package org.zwobble.couscous.frontends.java;

import com.google.common.collect.ImmutableList;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.zwobble.couscous.ast.*;
import org.zwobble.couscous.ast.sugar.Lambda;

import java.util.List;

import static org.zwobble.couscous.ast.MethodCallNode.methodCall;
import static org.zwobble.couscous.ast.ReturnNode.returns;
import static org.zwobble.couscous.ast.StaticMethodCallNode.staticMethodCall;
import static org.zwobble.couscous.ast.sugar.Lambda.lambda;
import static org.zwobble.couscous.frontends.java.JavaTypes.typeOf;
import static org.zwobble.couscous.util.ExtraLists.eagerMap;

public class JavaExpressionMethodReferenceReader {
    private final JavaReader javaReader;

    public JavaExpressionMethodReferenceReader(JavaReader javaReader) {
        this.javaReader = javaReader;
    }

    public Lambda toLambda(Scope scope, ExpressionMethodReference expression) {
        IMethodBinding functionalInterfaceMethod = expression.resolveTypeBinding().getFunctionalInterfaceMethod();
        List<FormalArgumentNode> formalArguments = formalArguments(scope, functionalInterfaceMethod);

        return lambda(
            formalArguments,
            ImmutableList.of(returns(JavaExpressionReader.coerceExpression(
                typeOf(functionalInterfaceMethod.getReturnType()),
                generateValue(scope, expression, formalArguments)))));
    }

    private List<FormalArgumentNode> formalArguments(Scope scope, IMethodBinding functionalInterfaceMethod) {
        ImmutableList.Builder<FormalArgumentNode> arguments = ImmutableList.builder();

        ITypeBinding[] parameterTypes = functionalInterfaceMethod.getParameterTypes();
        for (int index = 0; index < parameterTypes.length; index++) {
            ITypeBinding parameterType = parameterTypes[index];
            arguments.add(scope.formalArgument("arg" + index, typeOf(parameterType)));
        }

        return arguments.build();
    }

    private ExpressionNode generateValue(Scope scope, ExpressionMethodReference expression, List<FormalArgumentNode> formalArguments) {
        IMethodBinding methodBinding = expression.resolveMethodBinding();
        String methodName = expression.getName().getIdentifier();
        List<ExpressionNode> arguments = eagerMap(formalArguments, VariableReferenceNode::reference);
        TypeName type = typeOf(methodBinding.getReturnType());

        if (Modifier.isStatic(methodBinding.getModifiers())) {
            return staticMethodCall(
                typeOf(methodBinding.getDeclaringClass()),
                methodName,
                arguments,
                type);
        } else {
            return methodCall(
                javaReader.readExpressionWithoutBoxing(scope, expression.getExpression()),
                methodName,
                arguments,
                type);
        }
    }
}
