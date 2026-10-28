package tech.raaf.structure.graph;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class ConstraintSerializer extends StdSerializer<Constraint> {

  public ConstraintSerializer() {
    super(Constraint.class);
  }

  @Override
  public void serialize(Constraint value, JsonGenerator gen, SerializationContext provider) {
    gen.writeStartObject();
    gen.writeObjectPropertyStart(value.getForRelationship());

    gen.writeStringProperty("cardinality", value.getCardinality().toString());
    gen.writeStringProperty("from", value.getStartOfConstraint().getUuid());
    gen.writeStringProperty("to", value.getEndOfConstraint().getUuid());

    gen.writeEndObject();
    gen.writeEndObject();
  }
}
