package org.zwobble.couscous.backends.python;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.zwobble.couscous.ast.TypeName;
import org.zwobble.couscous.ast.ClassNode;

import static java.util.Arrays.asList;
import static org.zwobble.couscous.backends.python.PythonCodeGenerator.generateCode;
import static org.zwobble.couscous.backends.python.PythonSerializer.serialize;

import lombok.SneakyThrows;
import lombok.val;

public class PythonCompiler {
    private final Path root;
    private final String packageName;

    public PythonCompiler(Path root, String packageName) {
        this.root = root;
        this.packageName = packageName;
    }
    
    public void compile(List<ClassNode> classes) {
        for (val classNode : classes) {
            compileClass(classNode);
        }
        writeClass(TypeName.of("java.lang.Integer"),
            "class Integer(object):\n" +
            "    def parseInt(value):\n" +
            "        return int(value)"
        );
    }

    private Path pathForClass(TypeName className) {
        return root
            .resolve(packageName)
            .resolve(className.getQualifiedName().replace(".", File.separator) + ".py");
    }

    private void compileClass(ClassNode classNode) {
        writeClass(classNode.getName(), serialize(generateCode(classNode)));
    }

    @SneakyThrows
    private void writeClass(TypeName name, String contents) {
        val path = pathForClass(name);
        Files.createDirectories(path.getParent());
        createPythonPackages(path.getParent());
        Files.write(path, asList(contents));
    }

    private void createPythonPackages(Path packagePath) throws IOException {
        while (packagePath.startsWith(root)) {
            val packageFile = packagePath.resolve("__init__.py");
            if (!packageFile.toFile().exists()) {
                Files.createFile(packageFile);
            }
            packagePath = packagePath.getParent();
        }
    }
}