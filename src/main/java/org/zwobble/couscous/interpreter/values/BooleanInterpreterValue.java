package org.zwobble.couscous.interpreter.values;

import java.util.Collections;
import java.util.Optional;
import org.zwobble.couscous.interpreter.NoSuchField;
import org.zwobble.couscous.values.PrimitiveValue;
import org.zwobble.couscous.values.PrimitiveValues;

public final class BooleanInterpreterValue implements InterpreterValue {
    public static final BooleanInterpreterValue TRUE = new BooleanInterpreterValue(true);
    public static final BooleanInterpreterValue FALSE = new BooleanInterpreterValue(false);

    public static BooleanInterpreterValue of(final boolean value) {
        return value ? TRUE : FALSE;
    }

    private static final ConcreteType TYPE = ConcreteType.builder(BooleanInterpreterValue.class, "boolean")
        
        .method("negate", Collections.emptyList(), (environment, arguments) ->
            of(!arguments.getReceiver().getValue()))
        
        .build();

    private final boolean value;

    private BooleanInterpreterValue(final boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
    
    @Override
    public ConcreteType getType() {
        return TYPE;
    }
    
    @Override
    public Optional<PrimitiveValue> toPrimitiveValue() {
        return Optional.of(PrimitiveValues.value(value));
    }
    
    @Override
    public InterpreterValue getField(String fieldName) {
        throw new NoSuchField(fieldName);
    }
    
    @Override
    public void setField(String fieldName, InterpreterValue value) {
        throw new NoSuchField(fieldName);
    }
    
    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof BooleanInterpreterValue)) return false;
        final BooleanInterpreterValue other = (BooleanInterpreterValue)o;
        if (this.getValue() != other.getValue()) return false;
        return true;
    }
    
    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.getValue() ? 79 : 97);
        return result;
    }
    
    @java.lang.Override
    public java.lang.String toString() {
        return "BooleanInterpreterValue(value=" + this.getValue() + ")";
    }
}