package org.openpcf.universal.graph;

import org.neo4j.graphdb.Direction;
import org.openpcf.universal.base.Context;
import org.openpcf.universal.graph.repositories.InstanceNodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Give a description of InstanceNode here.
 *
 * @author Rubin Simons <rubin.simons@raaftech.com>
 * @since 2014-11-27
 */
@NodeEntity
public class InstanceNode extends Node {

    @RelatedTo(type="HAS_RESULT", direction = Direction.OUTGOING)
    private Set<InstanceNode> results = new HashSet<>();

    @RelatedTo(type="HAS_COMPONENT", direction = Direction.OUTGOING)
    private Set<InstanceNode> components = new HashSet<>();

    @RelatedTo(type="HAS_GROUPING", direction = Direction.OUTGOING)
    private Set<InstanceNode> groupings = new HashSet<>();

    @RelatedTo(type="HAS_OPERATION", direction = Direction.OUTGOING)
    private Set<InstanceNode> operations = new HashSet<>();

    @RelatedTo(type="HAS_HANDLER", direction = Direction.OUTGOING)
    private Set<InstanceNode> handlers = new HashSet<>();

    @RelatedTo(type="HAS_PROTOCOL", direction = Direction.OUTGOING)
    private Set<InstanceNode> protocols = new HashSet<>();

    @RelatedTo(type="HAS_CAPABILITY", direction = Direction.OUTGOING)
    private Set<InstanceNode> capabilities = new HashSet<>();

    @RelatedTo(type="HAS_ACTOR", direction = Direction.OUTGOING)
    private Set<InstanceNode> actors = new HashSet<>();

    @RelatedTo(type="INSTANCE_OF", direction=Direction.OUTGOING)
    private TypeNode instanceOf;

    @RelatedTo(type="HAS_COMPONENT", direction=Direction.OUTGOING)
    private Set<InstanceNode> hasComponents;

    InstanceNodeRepository instanceNodeRepository = Context.INSTANCE.getInstanceNodeRepository();

    DynamicProperties kv = new DynamicPropertiesContainer();

    private TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();

    public InstanceNode() {}

    public InstanceNode(String name, String desc, TypeNode instanceOf) {
        super(name, desc);
        this.instanceOf = instanceOf;
    }

    public InstanceNode(String name, String desc, String instanceOfUuid) {
        super(name, desc);

        TypeNode instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        if (instanceOf != null) {
            this.instanceOf = typeNodeRepository.findByUuid(instanceOfUuid);
        } else {
            throw new RuntimeException("ERROR: can't create InstanceNode, UUID " + instanceOfUuid + " unknown.");
        }
    }

    public void addResult(InstanceNode result) {
        if (results == null) {
            results = new HashSet<>();
        }
        results.add(result);
    }

    public void addResult(String resultUuid) {
        InstanceNode result = null;

        if (resultUuid != null) {
            result = instanceNodeRepository.findByUuid(resultUuid);
            if (result != null) {
                addResult(result);
            }
        }
    }

    public InstanceNode setProperties(Map<String, String> properties) {
        HashMap<String, Descriptor> schema = instanceOf.getSchema();

        schema.forEach((key, descriptor) -> {
            if (properties.containsKey(key)) {
                // validate
                // TODO: create a pluggable validation system
                boolean valid = false;

                String value = properties.get(key);

                switch(descriptor.getValueType()) {
                    case "datetime":
                        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
                        LocalDateTime.parse(value);
                        valid = true;
                        break;
                    case "string":
                        valid = true;
                        break;
                    case "integer":
                        Integer.parseInt(value);
                        valid = true;
                        break;
                    case "double":
                        Double.parseDouble(value);
                        break;
                    case "boolean":
                        if (value.equals("true") || value.equals("false")) {
                            valid = true;
                        }
                        break;
                }
                if (valid) {
                    kv.setProperty(key, properties.get(key));
                    properties.remove(key);
                } else {
                    throw new RuntimeException("Value for key '" + key + "' not of type '" + descriptor.getValueType() +
                            "'");
                }
            } else {
                if (descriptor.getRequired() == true) {
                    // this is a required key that isn't supplied
                    throw new RuntimeException("Can't create InstanceNode, missing property '" + key +"'");
                } else {
                    // this is an optional key that isn't supplied, use default value from TypeNode
                    kv.setProperty(key, descriptor.getDefaultValue());
                    properties.remove(key);
                }
            }
        });

        // properties should be empty by now, if not, unsupported properties were supplied.
        if (properties.size() > 0) {
            throw new RuntimeException("Can't create instanceNode, unsupported properties supplied");
        }

        return this;
    }

    public InstanceNode save() {
        if (instanceNodeRepository.findByUuid(getUuid()) == null) {
            instanceNodeRepository.save(this);
        }
        return this;
    }
}
