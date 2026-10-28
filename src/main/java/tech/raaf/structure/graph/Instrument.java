package tech.raaf.structure.graph;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import tools.jackson.databind.node.ObjectNode;

public abstract class Instrument extends Instance {

  private static final String WEIGHT = "weight";

  public Instrument() {}

  public Instrument(ObjectNode json, Type type) {
    super(json, type);
  }

  public int getWeight() {
    return data.containsKey(WEIGHT) ? ((Number) data.get(WEIGHT)).intValue() : 0;
  }

  public Instrument save() {
    Graph.transaction(this::write);
    return this;
  }

  // The relations of a new instrument to its types
  protected Entity toEntity(Type capability, Set<Type> appliesTo, Type providedBy) {
    final Entity entity = super.toEntity();
    if (!persisted) {
      if (capability != null) {
        entity.addRelations("GIVES_CAPABILITY", Direction.OUTGOING, uuids(Collections.singleton(capability)));
      }
      if (appliesTo != null && !appliesTo.isEmpty()) {
        entity.addRelations("APPLIES_TO", Direction.OUTGOING, uuids(appliesTo));
      }
      if (providedBy != null) {
        entity.addRelations("PROVIDED_BY", Direction.OUTGOING, uuids(Collections.singleton(providedBy)));
      }
    }
    return entity;
  }

  private static Set<UUID> uuids(Set<Type> types) {
    return types
      .stream()
      .map(t -> UUID.fromString(t.getUuid()))
      .collect(Collectors.toSet());
  }

  public ObjectNode toJson() {
    ObjectNode json = super.toJson();
    if (getWeight() > 0) {
      json.put("weight", getWeight());
    }
    return json;
  }
}
