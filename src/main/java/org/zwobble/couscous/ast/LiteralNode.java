package org.zwobble.couscous.ast;

import org.zwobble.couscous.ast.visitors.ExpressionNodeVisitor;
import org.zwobble.couscous.values.BooleanValue;
import org.zwobble.couscous.values.IntegerValue;
import org.zwobble.couscous.values.InterpreterValue;
import org.zwobble.couscous.values.StringValue;

import lombok.Value;

@Value
public class LiteralNode implements ExpressionNode {
    public static LiteralNode literal(String value) {
        return new LiteralNode(new StringValue(value));
    }
    
    public static LiteralNode literal(int value) {
        return new LiteralNode(new IntegerValue(value));
    }
    
    public static LiteralNode literal(boolean value) {
        return new LiteralNode(new BooleanValue(value));
    }
    
    InterpreterValue value;

    @Override
    public <T> T accept(ExpressionNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
