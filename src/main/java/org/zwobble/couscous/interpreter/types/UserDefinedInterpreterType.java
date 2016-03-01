package org.zwobble.couscous.interpreter.types;

import org.zwobble.couscous.ast.*;
import org.zwobble.couscous.interpreter.Environment;
import org.zwobble.couscous.interpreter.Executor;
import org.zwobble.couscous.interpreter.PositionalArguments;
import org.zwobble.couscous.interpreter.errors.NoSuchMethod;
import org.zwobble.couscous.interpreter.values.InterpreterValue;
import org.zwobble.couscous.interpreter.values.ObjectInterpreterValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Iterables.filter;
import static org.zwobble.couscous.util.Casts.tryCast;
import static org.zwobble.couscous.util.ExtraLists.list;
import static org.zwobble.couscous.util.ExtraMaps.map;
import static org.zwobble.couscous.util.ExtraMaps.toMapWithKeys;

public class UserDefinedInterpreterType implements InterpreterType {
    private final TypeNode type;
    private final Map<String, FieldDeclarationNode> fields;
    private final Map<MethodSignature, MethodNode> methods;

    public UserDefinedInterpreterType(TypeNode type) {
        this.type = type;
        this.fields = tryCast(ClassNode.class, type)
            .map(classNode -> toMapWithKeys(classNode.getFields(), field -> field.getName()))
            .orElse(map());
        this.methods = toMapWithKeys(
            filter(type.getMethods(), method -> !method.isAbstract()),
            method -> method.signature());
    }

    @Override
    public TypeName getName() {
        return type.getName();
    }

    @Override
    public Set<TypeName> getSuperTypes() {
        return type.getSuperTypes();
    }

    @Override
    public Optional<FieldDeclarationNode> getField(String fieldName) {
        return Optional.ofNullable(fields.get(fieldName));
    }

    @Override
    public List<StatementNode> getStaticConstructor() {
        return tryCast(ClassNode.class, type)
            .map(node -> node.getStaticConstructor())
            .orElse(list());
    }

    @Override
    public InterpreterValue callConstructor(Environment environment, List<InterpreterValue> arguments) {
        ConstructorNode constructor = tryCast(ClassNode.class, type)
            // TODO: add test for this case
            .orElseThrow(() -> new RuntimeException("Cannot instantiate non-class types"))
            .getConstructor();
        InterpreterValue object = new ObjectInterpreterValue(this);
        Executor.callConstructor(
            environment,
            constructor,
            object,
            new PositionalArguments(arguments));
        return object;
    }

    @Override
    public InterpreterValue callMethod(Environment environment, InterpreterValue value, MethodSignature signature, List<InterpreterValue> arguments) {
        return Executor.callMethod(
            environment,
            findMethod(signature, false),
            Optional.of(value),
            new PositionalArguments(arguments));
    }

    @Override
    public InterpreterValue callStaticMethod(Environment environment, MethodSignature signature, List<InterpreterValue> arguments) {
        return Executor.callMethod(
            environment,
            findMethod(signature, true),
            Optional.empty(),
            new PositionalArguments(arguments));
    }

    private MethodNode findMethod(MethodSignature signature, boolean isStatic) {
        return Optional.ofNullable(methods.get(signature))
            .filter(method -> method.isStatic() == isStatic)
            .orElseThrow(() -> new NoSuchMethod(signature));
    }
}