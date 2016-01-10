package org.zwobble.couscous.ast;

import java.util.List;
import java.util.Objects;

public class MethodSignature {
    private String name;
    private List<TypeName> arguments;

    public MethodSignature(String name, List<TypeName> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    public String getName() {
        return name;
    }

    public List<TypeName> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        return "MethodSignature(" +
            "name='" + name + '\'' +
            ", arguments=" + arguments +
            ')';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodSignature that = (MethodSignature) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arguments);
    }
}