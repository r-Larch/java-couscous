package org.zwobble.couscous.ast;

import java.util.List;

import org.zwobble.couscous.ast.visitors.NodeVisitor;

import com.google.common.collect.ImmutableList;

import lombok.Singular;
import lombok.Value;

@Value
public class ClassNode implements Node {
    public static ClassNode.Builder builder(String name) {
        return new Builder(name);
    }
    
    public static class Builder {
        private final String name;
        private final ImmutableList.Builder<MethodNode> methods;
        
        public Builder(String name) {
            this.name = name;
            this.methods = ImmutableList.builder();
        }
        
        public Builder method(MethodNode method) {
            this.methods.add(method);
            return this;
        }
        
        public ClassNode build() {
            return new ClassNode(name, methods.build());
        }
    }
    
    String name;
    @Singular
    List<MethodNode> methods;
    
    public String getLocalName() {
        // TODO Stronger notion of class names? Should look up the terminology
        return name.substring(name.lastIndexOf(".") + 1);
    }

    @Override
    public void accept(NodeVisitor visitor) {
        visitor.visit(this);
    }
}
