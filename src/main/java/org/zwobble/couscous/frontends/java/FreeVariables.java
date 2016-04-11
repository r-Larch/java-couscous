package org.zwobble.couscous.frontends.java;

import com.google.common.collect.ImmutableSet;
import org.zwobble.couscous.ast.*;
import org.zwobble.couscous.ast.visitors.NodeMapperWithDefault;
import org.zwobble.couscous.types.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zwobble.couscous.ast.structure.NodeStructure.descendantNodesAndSelf;
import static org.zwobble.couscous.util.Casts.tryCast;
import static org.zwobble.couscous.util.ExtraIterables.iterable;
import static org.zwobble.couscous.util.ExtraIterables.lazyFlatMap;
import static org.zwobble.couscous.util.ExtraLists.list;
import static org.zwobble.couscous.util.ExtraStreams.toStream;

public class FreeVariables {
    public static Set<TypeParameter> findFreeTypeParameters(Node root) {
        // TODO: this assumes that *all* type parameters are free, which is often but not always true in inner types
        Iterable<Type> types = iterable(() -> descendantNodesAndSelf(root)
            .flatMap(node -> node.accept(new NodeMapperWithDefault<Stream<Type>>(Stream.empty()) {
                @Override
                public Stream<Type> visit(FieldDeclarationNode declaration) {
                    return Stream.of(declaration.getType());
                }

                @Override
                public Stream<Type> visit(LocalVariableDeclarationNode localVariableDeclaration) {
                    return Stream.of(localVariableDeclaration.getType());
                }

                @Override
                public Stream<Type> visit(MethodNode methodNode) {
                    return Stream.concat(
                        methodNode.getArguments().stream().map(argument -> argument.getType()),
                        Stream.of(methodNode.getReturnType()));
                }

                @Override
                public Stream<Type> visit(ConstructorCallNode call) {
                    return Stream.of(call.getType());
                }

                @Override
                public Stream<Type> visit(MethodCallNode methodCall) {
                    return methodCall.getTypeParameters().stream();
                }
            })));

        return ImmutableSet.copyOf(lazyFlatMap(types, type -> type.accept(new Type.Visitor<Iterable<TypeParameter>>() {
            @Override
            public Iterable<TypeParameter> visit(ScalarType type) {
                return list();
            }

            @Override
            public Iterable<TypeParameter> visit(TypeParameter parameter) {
                return list(parameter);
            }

            @Override
            public Iterable<TypeParameter> visit(ParameterizedType type) {
                return lazyFlatMap(type.getParameters(), parameter -> parameter.accept(this));
            }

            @Override
            public Iterable<TypeParameter> visit(BoundTypeParameter type) {
                return type.getValue().accept(this);
            }
        })));
    }

    public static List<ReferenceNode> findFreeVariables(List<? extends Node> body) {
        Set<VariableDeclaration> declarations = body.stream()
            .flatMap(FreeVariables::findDeclarations)
            .collect(Collectors.toSet());

        return body.stream()
            .flatMap(FreeVariables::findReferences)
            .filter(reference ->
                tryCast(VariableReferenceNode.class, reference)
                    .map(variableReference -> !declarations.contains(variableReference.getReferent()))
                    .orElse(true))
            .collect(Collectors.toList());
    }

    private static Stream<ReferenceNode> findReferences(Node root) {
        return descendantNodesAndSelf(root).flatMap(node ->
            toStream(tryCast(ReferenceNode.class, node)));
    }

    private static Stream<VariableDeclaration> findDeclarations(Node root) {
        Stream<VariableNode> declarations = descendantNodesAndSelf(root).flatMap(node ->
            toStream(tryCast(VariableNode.class, node)));
        return declarations.map(VariableNode::getDeclaration);
    }
}
