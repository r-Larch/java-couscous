package org.zwobble.couscous.ast;

import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;

import static org.zwobble.couscous.ast.FormalArgumentNode.formalArg;
import static org.zwobble.couscous.ast.VariableDeclaration.var;

public class ClassNodeBuilder {
    private final TypeName name;
    private final ImmutableList.Builder<FieldDeclarationNode> fields;
    private Optional<ConstructorNode> constructor;
    private final ImmutableList.Builder<MethodNode> methods;
    
    public ClassNodeBuilder(String name) {
        this.name = TypeName.of(name);
        this.fields = ImmutableList.builder();
        this.constructor = Optional.empty();
        this.methods = ImmutableList.builder();
    }

    public ClassNodeBuilder field(String name, TypeName type) {
        this.fields.add(FieldDeclarationNode.field(name, type));
        return this;
    }

    public ClassNodeBuilder callable(CallableNode callable) {
        if (callable instanceof ConstructorNode) {
            return constructor((ConstructorNode) callable);
        } else {
            return method((MethodNode) callable);
        }
    }

    public ClassNodeBuilder constructor(ConstructorNode constructor) {
        this.constructor = Optional.of(constructor);
        return this;
    }
    
    public ClassNodeBuilder constructor(Function<MethodBuilder<ConstructorNode>, MethodBuilder<ConstructorNode>> build) {
        return constructor(build.apply(constructorBuilder()).build());
    }

    private MethodBuilder<ConstructorNode> constructorBuilder() {
        return new MethodBuilder<>(builder ->
            ConstructorNode.constructor(
                builder.arguments.build(),
                builder.statements.build()));
    }

    public ClassNodeBuilder method(MethodNode method) {
        this.methods.add(method);
        return this;
    }
    
    public ClassNodeBuilder staticMethod(String name, Function<MethodBuilder<MethodNode>, MethodBuilder<MethodNode>> build) {
        return method(name, true, build);
    }
    
    public ClassNodeBuilder method(String name, Function<MethodBuilder<MethodNode>, MethodBuilder<MethodNode>> build) {
        return method(name, false, build);
    }
    
    public ClassNodeBuilder method(String name, boolean isStatic, Function<MethodBuilder<MethodNode>, MethodBuilder<MethodNode>> build) {
        this.methods.add(build.apply(methodBuilder(name, isStatic)).build());
        return this;
    }

    private MethodBuilder<MethodNode> methodBuilder(String name, boolean isStatic) {
        return new MethodBuilder<>(builder -> MethodNode.builder(name)
            .isStatic(isStatic)
            .annotations(builder.annotations.build())
            .arguments(builder.arguments.build())
            .body(builder.statements.build())
            .build());
    }
    
    public ClassNode build() {
        return ClassNode.declareClass(
            name,
            fields.build(),
            constructor.orElse(constructorBuilder().build()),
            methods.build());
    }

    public class MethodBuilder<T> {
        private final Function<MethodBuilder<?>, T> build;
        private final ImmutableList.Builder<AnnotationNode> annotations;
        private final ImmutableList.Builder<FormalArgumentNode> arguments;
        private final ImmutableList.Builder<StatementNode> statements;

        public MethodBuilder(Function<MethodBuilder<?>, T> build) {
            this.build = build;
            this.annotations = ImmutableList.builder();
            this.arguments = ImmutableList.builder();
            this.statements = ImmutableList.builder();
        }
        
        public MethodBuilder<T> annotation(TypeName type) {
            annotations.add(AnnotationNode.annotation(type));
            return this;
        }
        
        public ThisReferenceNode thisReference() {
            return ThisReferenceNode.thisReference(name);
        }
        
        public MethodBuilder<T> argument(String id, String name, TypeName type) {
            arguments.add(formalArg(var(id, name, type)));
            return this;
        }
        
        public MethodBuilder<T> argument(FormalArgumentNode argument) {
            arguments.add(argument);
            return this;
        }
        
        public MethodBuilder<T> statement(StatementNode statement) {
            statements.add(statement);
            return this;
        }

        public T build() {
            return this.build.apply(this);
        }
    }
}