package tech.raaf.structure.graph;

import static io.vertx.core.json.Json.mapper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.exceptions.MarkerBuilderException;
import tech.raaf.structure.graph.repositories.TypeRepository;

public class Marker extends Instrument {

  // The types of a new marker; save() writes them
  private Type capability;
  private Set<Type> appliesTo;
  private Type providedBy;

  static final Logger log = LoggerFactory.getLogger(Marker.class);
  private static final TypeRepository typeRepository = Context.getTypeRepository();

  public static class Builder {

    private static final String NAME = "name";
    private static final String DESC = "desc";
    private static final String FILEPATH = "filePath";
    private static final String WEIGHT = "weight";

    private String name;
    private String desc;
    private String filePath;
    private Type capability;
    private Set<Type> appliesTo;
    private Type providedBy;

    public Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder desc(String desc) {
      this.desc = desc;
      return this;
    }

    public Builder filePath(String filePath) {
      this.filePath = filePath;
      return this;
    }

    public Builder capability(Type capability) {
      this.capability = capability;
      return this;
    }

    public Builder capability(String capabilityName) {
      Type capabilityType = typeRepository.findOneByName(capabilityName);
      return capability(capabilityType);
    }

    public Builder appliesTo(Set<Type> appliesTo) {
      this.appliesTo = appliesTo;
      return this;
    }

    public Builder providedBy(Type providedBy) {
      this.providedBy = providedBy;
      return this;
    }

    public Builder providedBy(String providedByName) {
      Type providedByType = typeRepository.findOneByName(providedByName);
      return providedBy(providedByType);
    }

    public Marker build() {
      if (
        name == null ||
        desc == null ||
        filePath == null ||
        capability == null ||
        (appliesTo == null) | (providedBy == null)
      ) {
        log.error("Error while building Marker instrument.");
        throw new MarkerBuilderException("Not all required parameters were set.");
      }
      Type markerType = typeRepository.findOneByName("Marker");
      final ObjectNode json = mapper
        .createObjectNode()
        .put(NAME, name)
        .put(DESC, desc)
        .put(FILEPATH, filePath)
        .put(WEIGHT, 0);
      Marker marker = new Marker(json, markerType);
      marker.capability = capability;
      marker.appliesTo = appliesTo;
      marker.providedBy = providedBy;
      return marker;
    }
  }

  public Marker() {
    // empty constructor for NodeMapper
  }

  @Override
  protected Entity toEntity() {
    return toEntity(capability, appliesTo, providedBy);
  }

  private Marker(ObjectNode json, Type type) {
    super(json, type);
  }
}
