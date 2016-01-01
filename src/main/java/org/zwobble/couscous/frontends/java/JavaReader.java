package org.zwobble.couscous.frontends.java;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.eclipse.jdt.core.dom.*;
import org.zwobble.couscous.ast.*;
import org.zwobble.couscous.ast.VariableDeclaration;
import org.zwobble.couscous.ast.identifiers.Identifier;
import org.zwobble.couscous.ast.sugar.Lambda;
import org.zwobble.couscous.util.ExtraLists;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.immutableEntry;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.zwobble.couscous.ast.AnnotationNode.annotation;
import static org.zwobble.couscous.ast.AssignmentNode.assignStatement;
import static org.zwobble.couscous.ast.ConstructorNode.constructor;
import static org.zwobble.couscous.ast.FieldAccessNode.fieldAccess;
import static org.zwobble.couscous.ast.FieldDeclarationNode.field;
import static org.zwobble.couscous.ast.FormalArgumentNode.formalArg;
import static org.zwobble.couscous.ast.ThisReferenceNode.thisReference;
import static org.zwobble.couscous.ast.VariableReferenceNode.reference;
import static org.zwobble.couscous.frontends.java.FreeVariables.findFreeVariables;
import static org.zwobble.couscous.frontends.java.JavaExpressionMethodReferenceReader.expressionMethodReferenceToLambda;
import static org.zwobble.couscous.frontends.java.JavaTypes.*;
import static org.zwobble.couscous.util.Casts.tryCast;
import static org.zwobble.couscous.util.ExtraLists.*;
import static org.zwobble.couscous.util.ExtraMaps.toMap;

public class JavaReader {
    public static List<ClassNode> readClassFromFile(Path root, Path sourcePath) throws IOException {
        CompilationUnit ast = new JavaParser().parseCompilationUnit(root, sourcePath);
        System.out.println(sourcePath);
        for (Message message : ast.getMessages()) {
            System.out.println(message.getMessage());
        }
        JavaReader reader = new JavaReader();
        return cons(reader.readCompilationUnit(ast), reader.classes.build());
    }

    private final ImmutableList.Builder<ClassNode> classes;
    private int anonymousClassCount = 0;

    private JavaReader() {
        classes = ImmutableList.builder();
    }

    private ClassNode readCompilationUnit(CompilationUnit ast) {
        TypeName name = generateClassName(ast);
        Scope scope = Scope.create().enterClass(name);
        TypeDeclaration type = (TypeDeclaration)ast.types().get(0);
        TypeDeclarationBody body = readTypeDeclarationBody(scope, type.bodyDeclarations());
        return ClassNode.declareClass(
            name,
            superTypes(type),
            body.getFields(),
            body.getConstructor(),
            body.getMethods());
    }

    GeneratedClosure readExpressionMethodReference(Scope outerScope, ExpressionMethodReference expression) {
        TypeName name = generateAnonymousName(expression);
        Scope scope = outerScope.enterClass(name);
        return generateClosure(
            scope,
            name,
            expression.resolveTypeBinding().getFunctionalInterfaceMethod(),
            expressionMethodReferenceToLambda(scope, expression));
    }

    GeneratedClosure readLambda(Scope outerScope, LambdaExpression expression) {
        TypeName name = generateAnonymousName(expression);
        Scope scope = outerScope.enterClass(name);
        return generateClosure(
            scope,
            name,
            expression.resolveTypeBinding().getFunctionalInterfaceMethod(),
            new JavaLambdaExpressionReader(this).toLambda(scope, expression));
    }

    GeneratedClosure generateClosure(Scope scope, TypeName name, IMethodBinding functionalInterfaceMethod, Lambda lambda) {
        MethodNode method = MethodNode.method(
            emptyList(),
            false,
            functionalInterfaceMethod.getName(),
            lambda.getFormalArguments(),
            lambda.getBody());

        GeneratedClosure closure = classWithCapture(
            scope,
            name,
            superTypesAndSelf(functionalInterfaceMethod.getDeclaringClass()),
            emptyList(),
            ImmutableList.of(method));

        classes.add(closure.getClassNode());
        return closure;
    }

    private List<StatementNode> replaceCaptureReferences(
        TypeName className,
        List<StatementNode> body,
        List<VariableDeclaration> freeVariables)
    {
        Map<Identifier, ExpressionNode> freeVariablesById = Maps.transformValues(
            Maps.uniqueIndex(freeVariables, VariableDeclaration::getId),
            freeVariable -> captureAccess(className, freeVariable));
        Function<ExpressionNode, ExpressionNode> replaceExpression = new CaptureReplacer(freeVariablesById);
        return eagerMap(body, statement -> statement.replaceExpressions(replaceExpression));
    }

    private class CaptureReplacer implements Function<ExpressionNode, ExpressionNode> {
        private final Map<Identifier, ExpressionNode> freeVariablesById;

        public CaptureReplacer(Map<Identifier, ExpressionNode> freeVariablesById) {
            this.freeVariablesById = freeVariablesById;
        }

        @Override
        public ExpressionNode apply(ExpressionNode expression) {
            return tryCast(VariableReferenceNode.class, expression)
                .flatMap(variableNode -> Optional.ofNullable(freeVariablesById.get(variableNode.getReferentId())))
                .orElseGet(() -> expression.replaceExpressions(this));
        }
    }

    private ConstructorNode buildConstructor(Scope outerScope, TypeName type, List<VariableDeclaration> freeVariables) {
        Scope scope = outerScope.enterConstructor();
        Map<Identifier, VariableDeclaration> argumentDeclarationsById = toMap(
            freeVariables,
            freeVariable -> immutableEntry(
                freeVariable.getId(),
                scope.generateVariable(freeVariable.getName(), freeVariable.getType())));

        List<FormalArgumentNode> arguments = eagerMap(
            freeVariables,
            freeVariable -> formalArg(argumentDeclarationsById.get(freeVariable.getId())));

        List<StatementNode> body = eagerMap(freeVariables, freeVariable -> assignStatement(
            captureAccess(type, freeVariable),
            reference(argumentDeclarationsById.get(freeVariable.getId()))));

        return constructor(arguments, body);
    }

    private FieldAccessNode captureAccess(TypeName type, VariableDeclaration freeVariable) {
        return fieldAccess(thisReference(type), freeVariable.getName(), freeVariable.getType());
    }

    GeneratedClosure readAnonymousClass(Scope outerScope, AnonymousClassDeclaration declaration) {
        TypeName className = generateAnonymousName(declaration);
        Scope scope = outerScope.enterClass(className);
        TypeDeclarationBody bodyDeclarations = readTypeDeclarationBody(scope, declaration.bodyDeclarations());
        GeneratedClosure closure = classWithCapture(
            scope,
            className,
            superTypes(declaration),
            bodyDeclarations.getFields(),
            bodyDeclarations.getMethods());
        classes.add(closure.getClassNode());
        return closure;
    }

    private GeneratedClosure classWithCapture(
        Scope scope,
        TypeName className,
        Set<TypeName> superTypes,
        List<FieldDeclarationNode> declaredFields,
        List<MethodNode> methods
    ) {
        List<VariableDeclaration> freeVariables = findFreeVariables(ExtraLists.concat(declaredFields, methods));
        Iterable<FieldDeclarationNode> captureFields = transform(
            freeVariables,
            freeVariable -> field(freeVariable.getName(), freeVariable.getType()));

        List<FieldDeclarationNode> fields = ImmutableList.copyOf(concat(declaredFields, captureFields));

        ClassNode classNode = ClassNode.declareClass(
            className,
            superTypes,
            fields,
            buildConstructor(scope, className, freeVariables),
            eagerMap(methods, method ->
                method.mapBody(body -> replaceCaptureReferences(className, body, freeVariables))));
        return new GeneratedClosure(classNode, freeVariables);
    }

    private TypeName generateAnonymousName(ASTNode node) {
        ITypeBinding type = findDeclaringClass(node);
        while (type.isAnonymous()) {
            type = type.getDeclaringClass();
        }
        return TypeName.of(type.getQualifiedName() + "_Anonymous_" + (anonymousClassCount++));
    }

    private ITypeBinding findDeclaringClass(ASTNode node) {
        while (!(node instanceof AbstractTypeDeclaration)) {
            node = node.getParent();
        }
        return ((AbstractTypeDeclaration)node).resolveBinding();
    }

    private static class TypeDeclarationBody {
        private final List<FieldDeclarationNode> fields;
        private final ConstructorNode constructor;
        private final List<MethodNode> methods;

        public TypeDeclarationBody(List<FieldDeclarationNode> fields, ConstructorNode constructor, List<MethodNode> methods) {
            this.fields = fields;
            this.constructor = constructor;
            this.methods = methods;
        }

        public List<FieldDeclarationNode> getFields() {
            return fields;
        }

        public ConstructorNode getConstructor() {
            return constructor;
        }

        public List<MethodNode> getMethods() {
            return methods;
        }
    }

    private TypeDeclarationBody readTypeDeclarationBody(Scope scope, List<Object> bodyDeclarations) {
        ImmutableList.Builder<MethodNode> methods = ImmutableList.builder();
        ConstructorNode constructor = ConstructorNode.DEFAULT;

        for (CallableNode callable : readMethods(scope, ofType(bodyDeclarations, MethodDeclaration.class))) {
            if (callable instanceof ConstructorNode) {
                constructor = (ConstructorNode) callable;
            } else {
                methods.add((MethodNode) callable);
            }
        }
        return new TypeDeclarationBody(
            readFields(ofType(bodyDeclarations, FieldDeclaration.class)),
            constructor,
            methods.build());
    }

    private List<FieldDeclarationNode> readFields(List<FieldDeclaration> fields) {
        return eagerFlatMap(fields, this::readField);
    }

    private List<FieldDeclarationNode> readField(FieldDeclaration field) {
        @SuppressWarnings("unchecked")
        List<VariableDeclarationFragment> fragments = field.fragments();
        TypeName type = typeOf(field.getType());
        return eagerMap(fragments, fragment ->
            field(fragment.getName().getIdentifier(), type));
    }

    private List<CallableNode> readMethods(Scope scope, List<MethodDeclaration> methods) {
        return eagerMap(methods, method -> readMethod(scope, method));
    }

    private CallableNode readMethod(Scope outerScope, MethodDeclaration method) {
        Scope scope = outerScope.enterMethod(method.getName().getIdentifier());

        List<FormalArgumentNode> formalArguments = readFormalArguments(scope, method);
        List<StatementNode> body = readBody(scope, method);
        List<AnnotationNode> annotations = readAnnotations(method);

        if (method.isConstructor()) {
            return constructor(
                formalArguments,
                body);
        } else {
            return MethodNode.method(
                annotations,
                Modifier.isStatic(method.getModifiers()),
                method.getName().getIdentifier(),
                formalArguments,
                body);
        }
    }

    private List<AnnotationNode> readAnnotations(MethodDeclaration method) {
        return eagerMap(asList(method.resolveBinding().getAnnotations()), this::readAnnotation);
    }

    private AnnotationNode readAnnotation(IAnnotationBinding annotationBinding) {
        return annotation(typeOf(annotationBinding.getAnnotationType()));
    }

    private List<FormalArgumentNode> readFormalArguments(Scope scope, MethodDeclaration method) {
        @SuppressWarnings("unchecked")
        List<SingleVariableDeclaration> parameters = method.parameters();
        return eagerMap(parameters, parameter -> JavaVariableDeclarationReader.read(scope, parameter));
    }

    private List<StatementNode> readBody(Scope scope, MethodDeclaration method) {
        @SuppressWarnings("unchecked")
        List<Statement> statements = method.getBody().statements();
        Optional<TypeName> returnType = Optional.ofNullable(method.getReturnType2())
            .map(Type::resolveBinding)
            .map(JavaTypes::typeOf);
        return readStatements(scope, statements, returnType);
    }

    List<StatementNode> readStatements(Scope scope, List<Statement> body, Optional<TypeName> returnType) {
        JavaStatementReader statementReader = new JavaStatementReader(scope, expressionReader(scope), returnType);
        return eagerFlatMap(body, statementReader::readStatement);
    }

    ExpressionNode readExpression(Scope scope, TypeName targetType, Expression body) {
        return expressionReader(scope).readExpression(targetType, body);
    }

    private TypeName generateClassName(CompilationUnit ast) {
        TypeDeclaration type = (TypeDeclaration)ast.types().get(0);
        String packageName = ast.getPackage().getName().getFullyQualifiedName();
        String className = type.getName().getFullyQualifiedName();
        return TypeName.of(packageName + "." + className);
    }

    private JavaExpressionReader expressionReader(Scope scope) {
        return new JavaExpressionReader(scope, this);
    }
}