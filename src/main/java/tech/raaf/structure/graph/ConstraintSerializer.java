package tech.raaf.structure.graph;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ConstraintSerializer extends StdSerializer<Constraint> {

    public ConstraintSerializer() {
        super(Constraint.class);
    }

    @Override
    public void serialize(Constraint value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectFieldStart(value.getForRelationship());

        gen.writeStringField("cardinality", value.getCardinality().toString());
        gen.writeStringField("from", value.getStartOfConstraint().getUuid());
        gen.writeStringField("to", value.getEndOfConstraint().getUuid());

        gen.writeEndObject();
        gen.writeEndObject();
    }
}
