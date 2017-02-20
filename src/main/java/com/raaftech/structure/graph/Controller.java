package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.exceptions.ComponentBuilderException;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.neo4j.graphdb.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static io.vertx.core.json.Json.mapper;

/**
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
public class Controller extends Instrument {

    private static final String NAME = "name";
    private static final String DESC = "desc";
    private static final String UUID = "uuid";
    private static final String FILELOCATION = "filePath";
    private static final String SUBJECTLOCAL = "subjectIsLocal";
    private static final String TEMPLATE = "template";
    private static final String TEMPLATEFILE = "templateFile";
    private static final String TEMPLATEFILE_CONTENT = "content";
    private static final String ARGUMENTS = "arguments";
    private static final String EXTENSION = "extension";
    private static final String RESULTPARSER = "resultParser";
    private static final String WEIGHT = "weight";

    @RelatedTo(type="GIVES_CAPABILITY", direction=Direction.OUTGOING)
    private Type capability;

    @RelatedTo(type="HANDLES", direction=Direction.OUTGOING)
    @Fetch
    private Set<Type> appliesTo;

    @RelatedTo(type="HAS_PARSER", direction=Direction.OUTGOING)
    private Parser parser;

    @RelatedTo(type="PRODUCED", direction=Direction.OUTGOING)
    @Fetch
    private Set<Result> results;

    @RelatedTo(type="HAS_INSTRUMENT", direction=Direction.INCOMING)
    @Fetch
    private Type providedBy;

    @Transient
    private static TypeRepository typeRepository = Context.getTypeRepository();

    private static final Logger log = LoggerFactory.getLogger(Controller.class);

    public static class Builder {
        // required fields
        private Type capability;
        private Set<Type> appliesTo;
        private Type providedBy;

        ObjectNode json = mapper.createObjectNode();

        public Builder() {}

        // required setters
        public Builder name(String name) {
            json.put(NAME, name);
            return this;
        }
        public Builder desc(String desc) {
            json.put(DESC, desc);
            return this;
        }
        public Builder capability(Type capability) {
            this.capability = capability;
            return this;
        }
        public Builder capability(String capabilityUuid) {
            capability(typeRepository.findOneByName(capabilityUuid));
            return this;
        }
        public Builder filePath(String filePath) {
            json.put(FILELOCATION, filePath);
            return this;
        }
        public Builder handles(Set<Type> handles) {
            this.appliesTo = handles;
            return this;
        }
        public Builder providedBy(Type providedBy) {
            this.providedBy = providedBy;
            return this;
        }
        public Builder providedBy(String providedByUuid) {
            providedBy(typeRepository.findOneByName(providedByUuid));
            return this;
        }
        public Builder subjectIsLocal(boolean subjectIsLocal) {
            json.put(SUBJECTLOCAL, subjectIsLocal);
            return this;
        }
        public Builder template(String template) {
            json.put(TEMPLATE, template);
            return this;
        }
        // optional setters
        public Builder uuid(String uuid) {
            json.put(UUID, uuid);
            return this;
        }
        public Builder arguments(String arguments) {
            json.put(ARGUMENTS, arguments);
            return this;
        }
        public Builder extension(String extension) {
            json.put(EXTENSION, extension);
            return this;
        }
        public Builder parser(Parser parser) {
            json.put(RESULTPARSER, parser.getUuid());
            return this;
        }
        public Builder weight(int weight) {
            json.put(WEIGHT, weight);
            return this;
        }

        public Controller build() {
            if (!json.has(NAME) || !json.has(DESC) || capability == null || !json.has(FILELOCATION)
                    || appliesTo == null || providedBy == null || !json.has(SUBJECTLOCAL) || !json.has(TEMPLATE)) {
                log.error("Error while building Controller instrument.");
                throw new ComponentBuilderException("Not all required parameters were set.");
            }
            Controller controller =  new Controller(json);
            controller.capability = capability;
            controller.appliesTo = appliesTo;
            controller.providedBy = providedBy;
            return controller;
        }

    }

    public Controller() {
        // empty constructor for Spring Data Neo4j
    }

    private Controller(ObjectNode json) {
        super(json, typeRepository.findOneByName("Controller"));
    }

    // Set the capability of a Controller instance
    public void setCapability(Type capability) {
        // Check argument for correctness
        if (capability.isOfType("Capability")) {
            this.capability = capability;
        } else {
            log.error("Can't set capability of controller " + getName() + " to " + capability.getName()
                    + " because it's not a capability.");
            throw new IllegalArgumentException("Illegal argument for setCapability()");
        }
    }

    // Set the capability of a controller instance
    public void setCapability(String capabilityUuid) {
        Type foundCapability = typeRepository.findByUuid(capabilityUuid);
        setCapability(foundCapability);
    }

    public Set<Type> getHandledTypes() {
        return appliesTo;
    }

    // Add a handled type to a controller
    public Controller addHandledType(Type type) {
        if (appliesTo == null) {
            appliesTo = new HashSet<>();
        }
        appliesTo.add(type);
        return this;
    }

    public String getArguments() {
        return (String) data.getProperty(ARGUMENTS);
    }

    public String getExtension() {
        return (String) data.getProperty(EXTENSION);
    }

    public boolean isSubjectIsLocal() {
        return  (boolean) data.getProperty(SUBJECTLOCAL);
    }

    public Type getProvidedBy() {
        return providedBy;
    }

    public String getTemplateText() {
        String content = null;
        try {
            JsonNode templateFile = mapper.readTree((String) file.getProperty(TEMPLATEFILE));
            content = templateFile.get(TEMPLATEFILE_CONTENT).asText();
        } catch (IOException e) {
            if (com.raaftech.structure.base.Configuration.isTraceModeOn()) {
                e.printStackTrace();
            }
            log.error("Error while parsing controller template for \"" + getName() + "\".");
        }
        return content;
    }


    public ObjectNode toJson() {
        ObjectNode json = super.toJson();
        json
                .put(SUBJECTLOCAL, isSubjectIsLocal())
                .put(TEMPLATE, getTemplateText());

        if (getExtension() != null) {
            json.put(EXTENSION, getExtension());
        }
        if (getArguments() != null) {
            json.put(ARGUMENTS, getArguments());
        }

        return json;
    }

}
