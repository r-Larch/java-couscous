package org.zwobble.couscous.backends.python;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import org.zwobble.couscous.ast.*;
import org.zwobble.couscous.ast.structure.NodeStructure;
import org.zwobble.couscous.ast.visitors.ExpressionNodeMapper;
import org.zwobble.couscous.ast.visitors.NodeMapperWithDefault;
import org.zwobble.couscous.ast.visitors.StatementNodeMapper;
import org.zwobble.couscous.backends.python.PrimitiveMethods.PrimitiveMethodGenerator;
import org.zwobble.couscous.backends.python.ast.*;
import org.zwobble.couscous.values.*;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Iterators.singletonIterator;
import static java.util.Arrays.asList;
import static org.zwobble.couscous.backends.python.ast.PythonAssignmentNode.pythonAssignment;
import static org.zwobble.couscous.backends.python.ast.PythonAttributeAccessNode.pythonAttributeAccess;
import static org.zwobble.couscous.backends.python.ast.PythonBooleanLiteralNode.pythonBooleanLiteral;
import static org.zwobble.couscous.backends.python.ast.PythonCallNode.pythonCall;
import static org.zwobble.couscous.backends.python.ast.PythonClassNode.pythonClass;
import static org.zwobble.couscous.backends.python.ast.PythonConditionalExpressionNode.pythonConditionalExpression;
import static org.zwobble.couscous.backends.python.ast.PythonFunctionDefinitionNode.pythonFunctionDefinition;
import static org.zwobble.couscous.backends.python.ast.PythonIfStatementNode.pythonIfStatement;
import static org.zwobble.couscous.backends.python.ast.PythonImportAliasNode.pythonImportAlias;
import static org.zwobble.couscous.backends.python.ast.PythonImportNode.pythonImport;
import static org.zwobble.couscous.backends.python.ast.PythonIntegerLiteralNode.pythonIntegerLiteral;
import static org.zwobble.couscous.backends.python.ast.PythonModuleNode.pythonModule;
import static org.zwobble.couscous.backends.python.ast.PythonReturnNode.pythonReturn;
import static org.zwobble.couscous.backends.python.ast.PythonStringLiteralNode.pythonStringLiteral;
import static org.zwobble.couscous.backends.python.ast.PythonVariableReferenceNode.pythonVariableReference;
import static org.zwobble.couscous.backends.python.ast.PythonWhileNode.pythonWhile;

public class PythonCodeGenerator {
    public static PythonModuleNode generateCode(ClassNode classNode) {
        Iterator<PythonImportNode> imports = generateImports(classNode).iterator();
        
        PythonImportNode internalsImport = pythonImport(
            importPathToRoot(classNode),
            asList(pythonImportAlias("_couscous")));
        
        PythonFunctionDefinitionNode constructor =
            generateConstructor(classNode.getConstructor());
        
        Iterable<PythonFunctionDefinitionNode> pythonMethods = Iterables.transform(
            classNode.getMethods(),
            PythonCodeGenerator::generateFunction);
        
        PythonClassNode pythonClass = pythonClass(
            classNode.getSimpleName(),
            ImmutableList.copyOf(Iterables.concat(asList(constructor), pythonMethods)));
        
        return pythonModule(ImmutableList.copyOf(Iterators.concat(
            singletonIterator(pythonClass),
            imports,
            singletonIterator(internalsImport))));
    }
    
    private static Stream<PythonImportNode> generateImports(ClassNode classNode) {
        Set<TypeName> classes = findReferencedClasses(classNode);
        return classes.stream()
            .filter(name -> !name.equals(InternalCouscousValue.REF))
            .map(name -> pythonImport(importPathToRoot(classNode) + name.getQualifiedName(), asList(pythonImportAlias(name.getSimpleName()))));
    }

    private static String importPathToRoot(ClassNode classNode) {
        return Strings.repeat(".", packageDepth(classNode) + 1);
    }
    
    private static int packageDepth(ClassNode classNode) {
        int depth = 0;
        final java.lang.String qualifiedName = classNode.getName().getQualifiedName();
        for (int index = 0; index < qualifiedName.length(); index++) {
            if (qualifiedName.charAt(index) == '.') {
                depth += 1;
            }
        }
        return depth;
    }
    
    private static Set<TypeName> findReferencedClasses(ClassNode classNode) {
        return NodeStructure.descendantNodes(classNode)
            .flatMap(node -> node.accept(new NodeMapperWithDefault<Stream<TypeName>>(Stream.empty()) {
                @Override
                public Stream<TypeName> visit(StaticMethodCallNode staticMethodCall) {
                    return Stream.of(staticMethodCall.getClassName());
                }

                @Override
                public Stream<TypeName> visit(ConstructorCallNode call) {
                    return Stream.of(call.getType());
                }
            }))
            .collect(Collectors.toSet());
    }
    
    public static PythonExpressionNode generateCode(PrimitiveValue value) {
        return value.accept(new PrimitiveValueVisitor<PythonExpressionNode>(){
            
            @Override
            public PythonExpressionNode visit(IntegerValue value) {
                return pythonIntegerLiteral(value.getValue());
            }
            
            @Override
            public PythonExpressionNode visit(StringValue value) {
                return pythonStringLiteral(value.getValue());
            }
            
            @Override
            public PythonExpressionNode visit(BooleanValue value) {
                return pythonBooleanLiteral(value.getValue());
            }
            
            @Override
            public PythonExpressionNode visit(UnitValue unitValue) {
                throw new UnsupportedOperationException();
            }
        });
    }
    
    private static PythonFunctionDefinitionNode generateConstructor(ConstructorNode constructor) {
        Iterable<String> explicitArgumentNames = Iterables.transform(constructor.getArguments(), argument -> argument.getName());
        Iterable<String> argumentNames = Iterables.concat(asList("self"), explicitArgumentNames);
        List<PythonStatementNode> pythonBody = constructor.getBody().stream().map(PythonCodeGenerator::generateStatement).collect(Collectors.toList());
        return pythonFunctionDefinition("__init__", ImmutableList.copyOf(argumentNames), new PythonBlock(pythonBody));
    }
    
    private static PythonFunctionDefinitionNode generateFunction(MethodNode method) {
        Iterable<String> explicitArgumentNames = Iterables.transform(method.getArguments(), argument -> argument.getName());
        Iterable<String> argumentNames = Iterables.concat(
            method.isStatic() ? Collections.<String>emptyList() : asList("self"),
            explicitArgumentNames);
        List<PythonStatementNode> pythonBody = generateStatements(method.getBody());
        return pythonFunctionDefinition(method.getName(), ImmutableList.copyOf(argumentNames), new PythonBlock(pythonBody));
    }
    
    private static List<PythonStatementNode> generateStatements(List<StatementNode> statements) {
        return statements.stream()
            .map(PythonCodeGenerator::generateStatement)
            .collect(Collectors.toList());
    }
    
    private static PythonStatementNode generateStatement(StatementNode statement) {
        return statement.accept(new StatementGenerator());
    }
    
    private static class StatementGenerator implements StatementNodeMapper<PythonStatementNode> {
        @Override
        public PythonStatementNode visit(ReturnNode returnNode) {
            return pythonReturn(generateExpression(returnNode.getValue()));
        }
        
        @Override
        public PythonStatementNode visit(ExpressionStatementNode expressionStatement) {
            if (expressionStatement.getExpression() instanceof AssignmentNode) {
                final org.zwobble.couscous.ast.AssignmentNode assignment = (AssignmentNode)expressionStatement.getExpression();
                return pythonAssignment(assignment.getTarget().accept(EXPRESSION_GENERATOR), generateExpression(assignment.getValue()));
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        @Override
        public PythonStatementNode visit(LocalVariableDeclarationNode declaration) {
            return pythonAssignment(pythonVariableReference(declaration.getDeclaration().getName()), generateExpression(declaration.getInitialValue()));
        }

        @Override
        public PythonStatementNode visit(IfStatementNode ifStatement) {
            return pythonIfStatement(
                generateExpression(ifStatement.getCondition()),
                generateStatements(ifStatement.getTrueBranch()),
                generateStatements(ifStatement.getFalseBranch()));
        }

        @Override
        public PythonStatementNode visit(WhileNode whileLoop) {
            return pythonWhile(
                generateExpression(whileLoop.getCondition()),
                generateStatements(whileLoop.getBody()));
        }
    }
    
    private static PythonExpressionNode generateExpression(ExpressionNode expression) {
        return expression.accept(EXPRESSION_GENERATOR);
    }
    
    private static List<PythonExpressionNode> generateExpressions(Iterable<? extends ExpressionNode> expressions) {
        return ImmutableList.copyOf(Iterables.transform(expressions, PythonCodeGenerator::generateExpression));
    }
    private static final ExpressionGenerator EXPRESSION_GENERATOR = new ExpressionGenerator();
    
    private static class ExpressionGenerator implements ExpressionNodeMapper<PythonExpressionNode> {
        @Override
        public PythonExpressionNode visit(LiteralNode literal) {
            return generateCode(literal.getValue());
        }
        
        @Override
        public PythonVariableReferenceNode visit(VariableReferenceNode variableReference) {
            return pythonVariableReference(variableReference.getReferent().getName());
        }
        
        @Override
        public PythonExpressionNode visit(ThisReferenceNode reference) {
            return pythonVariableReference("self");
        }
        
        @Override
        public PythonExpressionNode visit(AssignmentNode assignment) {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public PythonExpressionNode visit(TernaryConditionalNode ternaryConditional) {
            return pythonConditionalExpression(generateExpression(ternaryConditional.getCondition()), generateExpression(ternaryConditional.getIfTrue()), generateExpression(ternaryConditional.getIfFalse()));
        }
        
        @Override
        public PythonExpressionNode visit(MethodCallNode methodCall) {
            PythonExpressionNode receiver = generateExpression(methodCall.getReceiver());
            List<PythonExpressionNode> arguments = generateExpressions(methodCall.getArguments());
            if (isPrimitive(methodCall.getReceiver())) {
                PrimitiveMethodGenerator primitiveMethodGenerator = PrimitiveMethods.getPrimitiveMethod(methodCall.getReceiver().getType(), methodCall.getMethodName()).get();
                return primitiveMethodGenerator.generate(receiver, arguments);
            } else {
                return pythonCall(pythonAttributeAccess(receiver, methodCall.getMethodName()), arguments);
            }
        }
        
        @Override
        public PythonExpressionNode visit(StaticMethodCallNode staticMethodCall) {
            TypeName className = staticMethodCall.getClassName();
            PythonVariableReferenceNode classReference = pythonVariableReference(className.getSimpleName());
            PythonAttributeAccessNode methodReference = pythonAttributeAccess(classReference, staticMethodCall.getMethodName());
            List<PythonExpressionNode> arguments = generateExpressions(staticMethodCall.getArguments());
            
            return PrimitiveMethods.getPrimitiveStaticMethod(className, staticMethodCall.getMethodName())
                .map(generator -> generator.generate(arguments))
                .orElseGet(() -> pythonCall(methodReference, arguments));                
        }
        
        @Override
        public PythonExpressionNode visit(ConstructorCallNode call) {
            final org.zwobble.couscous.ast.TypeName className = call.getType();
            final org.zwobble.couscous.backends.python.ast.PythonVariableReferenceNode classReference = pythonVariableReference(className.getSimpleName());
            final java.util.List<org.zwobble.couscous.backends.python.ast.PythonExpressionNode> arguments = generateExpressions(call.getArguments());
            return pythonCall(classReference, arguments);
        }
        
        @Override
        public PythonExpressionNode visit(FieldAccessNode fieldAccess) {
            return pythonAttributeAccess(generateExpression(fieldAccess.getLeft()), fieldAccess.getFieldName());
        }

        @Override
        public PythonExpressionNode visit(TypeCoercionNode typeCoercion) {
            PythonExpressionNode value = generateExpression(typeCoercion.getExpression());
            if (typeCoercion.isIntegerBox()) {
                return internalMethod("boxInt", asList(value));
            } else if (typeCoercion.isIntegerUnbox()) {
                return internalMethod("unboxInt", asList(value));
            } else {
                return value;
            }
        }

        private PythonExpressionNode internalMethod(String name, List<PythonExpressionNode> arguments) {
            return pythonCall(pythonAttributeAccess(pythonVariableReference("_couscous"), name), arguments);
        }
    }
    
    private static boolean isPrimitive(ExpressionNode value) {
        return PrimitiveMethods.isPrimitive(value.getType());
    }
}