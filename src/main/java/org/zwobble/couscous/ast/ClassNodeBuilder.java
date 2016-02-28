package org.zwobble.couscous.ast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.zwobble.couscous.ast.identifiers.Identifier;
import org.zwobble.couscous.values.UnitValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.zwobble.couscous.ast.FormalArgumentNode.formalArg;
import static org.zwobble.couscous.ast.VariableDeclaration.var;
import static org.zwobble.couscous.util.ExtraLists.list;

public class ClassNodeBuilder {
    private final TypeName name;
    private final ImmutableSet.Builder<TypeName> superTypes;
    private final ImmutableList.Builder<FieldDeclarationNode> fields;
    private List<StatementNode> staticConstructor;
    private Optional<ConstructorNode> constructor;
    private final ImmutableList.Builder<MethodNode> methods;

    public ClassNodeBuilder(TypeName name) {
        this.name = name;
        this.superTypes = ImmutableSet.builder();
        this.fields = ImmutableList.builder();
        staticConstructor = list();
        this.constructor = Optional.empty();
        this.methods = ImmutableList.builder();
    }

    public ClassNodeBuilder(String name) {
        this(TypeName.of(name));
    }

    public ClassNodeBuilder addSuperType(String type) {
        this.superTypes.add(TypeName.of(type));
        return this;
    }

    public ClassNodeBuilder staticField(String name, TypeName type) {
        return field(FieldDeclarationNode.staticField(name, type));
    }

    public ClassNodeBuilder field(String name, TypeName type) {
        return field(FieldDeclarationNode.field(name, type));
    }

    public ClassNodeBuilder field(FieldDeclarationNode field) {
        this.fields.add(field);
        return this;
    }

    public ClassNodeBuilder constructor(ConstructorNode constructor) {
        this.constructor = Optional.of(constructor);
        return this;
    }
    
    public ClassNodeBuilder constructor(Function<MethodBuilder<ConstructorNode>, MethodBuilder<ConstructorNode>> build) {
        return constructor(build.apply(constructorBuilder()).build());
    }

    public ClassNodeBuilder staticConstructor(List<StatementNode> body) {
        this.staticConstructor = body;
        return this;
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
            .returns(builder.returnType)
            .body(builder.statements.build())
            .build());
    }
    
    public ClassNode build() {
        return ClassNode.declareClass(
            name,
            superTypes.build(),
            fields.build(),
            staticConstructor,
            constructor.orElse(ConstructorNode.DEFAULT),
            methods.build());
    }

    public class MethodBuilder<T> {
        private final Function<MethodBuilder<?>, T> build;
        private final ImmutableList.Builder<AnnotationNode> annotations;
        private final ImmutableList.Builder<FormalArgumentNode> arguments;
        private TypeName returnType = UnitValue.REF;
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
        
        public MethodBuilder<T> argument(Identifier id, String name, TypeName type) {
            arguments.add(formalArg(var(id, name, type)));
            return this;
        }

        public MethodBuilder<T> returns(TypeName returnType) {
            this.returnType = returnType;
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