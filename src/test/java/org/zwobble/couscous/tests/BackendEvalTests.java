package org.zwobble.couscous.tests;

import java.util.List;

import org.junit.Test;
import org.zwobble.couscous.ast.ClassNode;
import org.zwobble.couscous.ast.ExpressionNode;
import org.zwobble.couscous.ast.MethodNode;
import org.zwobble.couscous.ast.ReturnNode;
import org.zwobble.couscous.ast.TernaryConditionalNode;
import org.zwobble.couscous.values.IntegerValue;
import org.zwobble.couscous.values.PrimitiveValue;
import org.zwobble.couscous.values.StringValue;

import com.google.common.collect.ImmutableList;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.zwobble.couscous.ast.ConstructorCallNode.constructorCall;
import static org.zwobble.couscous.ast.LiteralNode.literal;
import static org.zwobble.couscous.ast.MethodCallNode.methodCall;
import static org.zwobble.couscous.ast.StaticMethodCallNode.staticMethodCall;
import static org.zwobble.couscous.values.PrimitiveValues.value;

import lombok.val;

public abstract class BackendEvalTests {
    @Test
    public void canEvaluateLiterals() {
        assertEquals(value("hello"), evalExpression(literal("hello")));
        assertEquals(value(true), evalExpression(literal(true)));
        assertEquals(value(false), evalExpression(literal(false)));
        assertEquals(value(42), evalExpression(literal(42)));
    }
    
    @Test
    public void whenConditionIsTrueThenValueOfConditionalTernaryIsTrueBranch() {
        val result = evalExpression(new TernaryConditionalNode(literal(true), literal("T"), literal("F")));
        assertEquals(value("T"), result);
    }
    
    @Test
    public void whenConditionIsFalseThenValueOfConditionalTernaryIsFalseBranch() {
        val result = evalExpression(new TernaryConditionalNode(literal(false), literal("T"), literal("F")));
        assertEquals(value("F"), result);
    }
    
    @Test
    public void canCallMethodWithNoArgumentsOnBuiltin() {
        val result = evalExpression(methodCall(
            literal("hello"),
            "length",
            asList(),
            IntegerValue.REF));
        assertEquals(value(5), result);
    }
    
    @Test
    public void canCallMethodWithArgumentsOnBuiltin() {
        val result = evalExpression(methodCall(
            literal("hello"),
            "substring",
            asList(literal(1), literal(4)),
            StringValue.REF));
        assertEquals(value("ell"), result);
    }
    
    @Test
    public void canCallBuiltinStaticMethod() {
        val result = evalExpression(
            staticMethodCall("java.lang.Integer", "parseInt", literal("42")));
        assertEquals(value(42), result);
    }
    
    @Test
    public void canCallStaticMethodFromUserDefinedStaticMethod() {
        val classNode = ClassNode.builder("com.example.Example")
            .method(MethodNode.staticMethod("main")
                .statement(new ReturnNode(staticMethodCall("java.lang.Integer", "parseInt", literal("42"))))
                .build())
            .build();
        val result = evalExpression(asList(classNode),
            staticMethodCall("com.example.Example", "main"));
        assertEquals(value(42), result);
    }
    
    @Test
    public void canCallInstanceMethodOnUserDefinedClass() {
        val classNode = ClassNode.builder("com.example.Example")
            .method(MethodNode.method("main")
                .statement(new ReturnNode(literal(42)))
                .build())
            .build();
        val result = evalExpression(asList(classNode),
            methodCall(constructorCall(classNode.getName(), asList()), "main", asList(), IntegerValue.REF));
        assertEquals(value(42), result);
    }
    
    protected PrimitiveValue evalExpression(ExpressionNode expression) {
        return evalExpression(ImmutableList.of(), expression);
    }
    
    protected abstract PrimitiveValue evalExpression(
        List<ClassNode> classes,
        ExpressionNode expression);
}
