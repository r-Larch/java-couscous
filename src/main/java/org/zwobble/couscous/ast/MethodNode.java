package org.zwobble.couscous.ast;

import com.google.common.collect.ImmutableList;
import org.zwobble.couscous.ast.visitors.NodeMapper;
import org.zwobble.couscous.values.UnitValue;

import java.util.List;
import java.util.function.Function;

import static org.zwobble.couscous.util.ExtraLists.eagerMap;

public class MethodNode implements CallableNode {
    public static Builder staticMethod(String name) {
        return builder(name).isStatic(true);
    }
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final ImmutableList.Builder<AnnotationNode> annotations =
            ImmutableList.builder();
        private boolean isStatic = false;
        private final String name;
        private final ImmutableList.Builder<FormalArgumentNode> arguments =
            ImmutableList.builder();
        private TypeName returnType = UnitValue.REF;
        private final ImmutableList.Builder<StatementNode> body =
            ImmutableList.builder();
        
        public Builder(String name) {
            this.name = name;
        }
        
        public Builder annotations(List<AnnotationNode> annotations) {
            this.annotations.addAll(annotations);
            return this;
        }
        
        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }
        
        public Builder argument(FormalArgumentNode argument) {
            arguments.add(argument);
            return this;
        }
        
        public Builder arguments(List<FormalArgumentNode> arguments) {
            this.arguments.addAll(arguments);
            return this;
        }
        
        public Builder statement(StatementNode statement) {
            this.body.add(statement);
            return this;
        }
        
        public Builder body(List<StatementNode> statements) {
            this.body.addAll(statements);
            return this;
        }
        
        public MethodNode build() {
            return new MethodNode(
                annotations.build(),
                isStatic,
                name,
                arguments.build(),
                returnType,
                body.build());
        }
    }
    
    public static MethodNode method(
        List<AnnotationNode> annotations,
        boolean isStatic,
        String name,
        List<FormalArgumentNode> arguments,
        TypeName returnType,
        List<StatementNode> body)
    {
        return new MethodNode(annotations, isStatic, name, arguments, returnType, body);
    }
    
    private final List<AnnotationNode> annotations;
    private final boolean isStatic;
    private final String name;
    private final List<FormalArgumentNode> arguments;
    private final TypeName returnType;
    private final List<StatementNode> body;
    
    private MethodNode(
        List<AnnotationNode> annotations,
        boolean isStatic,
        String name,
        List<FormalArgumentNode> arguments,
        TypeName returnType,
        List<StatementNode> body)
    {
        this.annotations = annotations;
        this.isStatic = isStatic;
        this.name = name;
        this.arguments = arguments;
        this.returnType = returnType;
        this.body = body;
    }
    
    public List<AnnotationNode> getAnnotations() {
        return annotations;
    }
    
    public boolean isStatic() {
        return isStatic;
    }
    
    public String getName() {
        return name;
    }
    
    public List<FormalArgumentNode> getArguments() {
        return arguments;
    }

    public TypeName getReturnType() {
        return returnType;
    }
    
    public List<StatementNode> getBody() {
        return body;
    }

    public MethodSignature signature() {
        return new MethodSignature(
            name,
            eagerMap(arguments, argument -> argument.getType()));
    }

    public MethodNode mapBody(Function<List<StatementNode>, List<StatementNode>> function) {
        return new MethodNode(annotations, isStatic, name, arguments, returnType, function.apply(body));
    }

    @Override
    public <T> T accept(NodeMapper<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "MethodNode(" +
            "annotations=" + annotations +
            ", isStatic=" + isStatic +
            ", name=" + name +
            ", arguments=" + arguments +
            ", returnType=" + returnType +
            ", body=" + body +
            ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodNode that = (MethodNode) o;

        if (isStatic != that.isStatic) return false;
        if (!annotations.equals(that.annotations)) return false;
        if (!name.equals(that.name)) return false;
        if (!arguments.equals(that.arguments)) return false;
        if (!returnType.equals(that.returnType)) return false;
        return body.equals(that.body);

    }

    @Override
    public int hashCode() {
        int result = annotations.hashCode();
        result = 31 * result + (isStatic ? 1 : 0);
        result = 31 * result + name.hashCode();
        result = 31 * result + arguments.hashCode();
        result = 31 * result + returnType.hashCode();
        result = 31 * result + body.hashCode();
        return result;
    }
}
