package com.raaftech.structure.graph;

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

import java.util.HashSet;
import java.util.Set;

public class Controller extends Instrument {

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

    private String extension;
    private String arguments;
    private boolean subjectIsLocal;
    private String template;

    @Transient
    private static TypeRepository typeRepository = Context.getTypeRepository();

    private Logger log = LoggerFactory.getLogger(Controller.class);

    public static class Builder {
        // required fields
        private String uuid;
        private String name;
        private String desc;
        private Type capability;
        private String filePath;
        private Set<Type> appliesTo;
        private Type providedBy;
        private Boolean subjectIsLocal;
        private String template;
        // optional fields
        private String arguments = null;
        private String extension = null;
        private Parser parser = null;
        private int weight = -1;

        public Builder() {}

        // required setters
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder desc(String desc) {
            this.desc = desc;
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
            this.filePath = filePath;
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
            this.subjectIsLocal = subjectIsLocal;
            return this;
        }
        public Builder template(String template) {
            this.template = template;
            return this;
        }
        // optional setters
        public Builder uuid(String uuid) {
            this.uuid = uuid;
            return this;
        }
        public Builder arguments(String arguments) {
            this.arguments = arguments;
            return this;
        }
        public Builder extension(String extension) {
            this.extension = extension;
            return this;
        }
        public Builder parser(Parser parser) {
            this.parser = parser;
            return this;
        }
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Controller build() {
            if (name == null || desc == null || capability == null || filePath == null
                    || appliesTo == null || providedBy == null || subjectIsLocal == null || template == null) {
                throw new ComponentBuilderException("Not all required parameters were set.");
            }
            Controller controller =  new Controller(name, desc, filePath);
            controller.capability = capability;
            controller.appliesTo = appliesTo;
            controller.providedBy = providedBy;
            controller.subjectIsLocal = subjectIsLocal;
            controller.template = template;
            if (arguments != null) {
                controller.arguments = arguments;
            }
            if (extension != null) {
                controller.extension = extension;
            }
            if (parser != null) {
                controller.parser = parser;
            }
            if (weight >= 0) {
                controller.weight = weight;
            }
            return controller;
        }

    }

    public Controller() {
        // empty constructor for Spring Data Neo4j
    }

    private Controller(String name, String desc, String uuid, String filePath) {
        super(name, desc, uuid, filePath);
    }
    private Controller(String name, String desc, String filePath) {
        super(name, desc, filePath);
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
        return arguments;
    }

    public String getExtension() {
        return extension;
    }

    public boolean isSubjectIsLocal() {
        return subjectIsLocal;
    }

    public Type getProvidedBy() {
        return providedBy;
    }

    public String getTemplate() {
        return template;
    }


    public ObjectNode toJson() {
        ObjectNode json = super.toJson();
        json
                .put("subjectIsLocal", subjectIsLocal)
                .put("template", template);

        if (extension != null) {
            json.put("extension", extension);
        }
        if (arguments != null) {
            json.put("arguments", arguments);
        }

        return json;
    }

}
