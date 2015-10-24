package org.zwobble.couscous.interpreter.values;

import java.util.Optional;

import org.zwobble.couscous.interpreter.NoSuchField;
import org.zwobble.couscous.values.PrimitiveValue;
import org.zwobble.couscous.values.PrimitiveValues;

import lombok.ToString;

@ToString
public class UnitInterpreterValue implements InterpreterValue {
    private static final ConcreteType TYPE = ConcreteType.builder(UnitInterpreterValue.class, "Unit")
        .build();
    
    public static final UnitInterpreterValue UNIT = new UnitInterpreterValue(); 
    
    private UnitInterpreterValue() {
    }

    @Override
    public ConcreteType getType() {
        return TYPE;
    }

    @Override
    public Optional<PrimitiveValue> toPrimitiveValue() {
        return Optional.of(PrimitiveValues.UNIT);
    }

    @Override
    public InterpreterValue getField(String fieldName) {
        throw new NoSuchField(fieldName);
    }

    @Override
    public void setField(String fieldName, InterpreterValue value) {
        throw new NoSuchField(fieldName);
    }
}