package org.zwobble.couscous.frontends.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.zwobble.couscous.Frontend;
import org.zwobble.couscous.ast.ClassNode;
import org.zwobble.couscous.util.ExtraLists;

public class JavaFrontend implements Frontend {
    @Override
    public List<ClassNode> readSourceDirectory(Path directoryPath) throws IOException {
        return ExtraLists.flatMap(
            findJavaFiles(directoryPath),
            javaFile -> JavaReader.readClassFromFile(directoryPath, javaFile));
    }
    
    private Stream<Path> findJavaFiles(Path directoryPath) throws IOException {
        return Files.walk(directoryPath)
            .filter(path -> path.toFile().isFile() && path.toString().endsWith(".java"));
    }
}