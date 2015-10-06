package org.zwobble.couscous.ast;

import java.util.List;

import org.zwobble.couscous.ast.visitors.NodeVisitor;

import lombok.Singular;
import lombok.Value;

@Value
public class ClassNode implements Node {
    public static ClassNodeBuilder builder(String name) {
        return new ClassNodeBuilder(name);
    }
    
    TypeName name;
    ConstructorNode constructor;
    @Singular
    List<MethodNode> methods;
    
    public String getSimpleName() {
        return name.getSimpleName();
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
