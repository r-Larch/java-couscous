package org.zwobble.couscous.tests.backends.csharp;

import org.junit.Test;
import org.zwobble.couscous.ast.MethodNode;
import org.zwobble.couscous.ast.Node;
import org.zwobble.couscous.ast.TypeName;
import org.zwobble.couscous.backends.csharp.CsharpSerializer;

import static org.junit.Assert.assertEquals;
import static org.zwobble.couscous.ast.LiteralNode.literal;
import static org.zwobble.couscous.ast.ReturnNode.returns;

public class CsharpSerializerTests {
    @Test
    public void integerLiterals() {
        String output = serialize(literal(42));
        assertEquals("42", output);
    }

    @Test
    public void booleanLiterals() {
        String trueOutput = serialize(literal(true));
        assertEquals("true", trueOutput);

        String falseOutput = serialize(literal(false));
        assertEquals("false", falseOutput);
    }

    @Test
    public void stringLiteralsUseDoubleQuotes() {
        String output = serialize(literal("blah"));
        assertEquals("\"blah\"", output);
    }

    @Test
    public void typeLiteralUsesTypeOfOperator() {
        String output = serialize(literal(TypeName.of("com.example.Example")));
        assertEquals("typeof(Couscous.com.example.Example)", output);
    }

    @Test
    public void returnStatementUsesReturnStatement() {
        String output = serialize(returns(literal(true)));
        assertEquals("return true;", output);
    }

    @Test
    public void methodHasDynamicReturnType() {
        String output = serialize(MethodNode.staticMethod("nothing").build());
        assertEquals("internal static dynamic nothing() {\n}\n", output);
    }

    @Test
    public void methodHasSerializedBody() {
        MethodNode method = MethodNode.staticMethod("nothing")
            .statement(returns(literal(true)))
            .build();
        String output = serialize(method);
        assertEquals("internal static dynamic nothing() {\n    return true;\n}\n", output);
    }

    private String serialize(Node node) {
        return CsharpSerializer.serialize(node, "Couscous");
    }
}
