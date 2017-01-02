package com.raaftech.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.repositories.TypeRepository;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

/**
 * @Deprecated We need to redesign the domain classes to use Entity.
 */
@Deprecated
public class Profile extends Instance {

    private static final TypeRepository typeRepository = Context.getTypeRepository();

    @RelatedTo(type="APPLIES_TO", direction= Direction.OUTGOING)
    @Fetch
    private Set<Type> appliesTo;

    @RelatedTo(type="PROVIDES", direction=Direction.INCOMING)
    @Fetch
    private Type providedBy;

    public static class Builder {
        ObjectNode data;
        public Builder(ObjectNode data) {
            this.data = data;
        }
        public Profile build() {
            Type profileType = typeRepository.findOneByName("Profile");
            return new Profile(data, profileType);

        }
    }

    public Profile() {
        // empty constructor for Spring Data Neo4j
    }

    private Profile(ObjectNode data, Type profileType) {
        super(data, profileType);
    }

    public void setAppliesTo(Set<Type> appliesTo) {
        if (this.appliesTo == null) {
            this.appliesTo = appliesTo;
        } else {
            throw new IllegalStateException("Can't set appliesTo, already set.");
        }
    }

    public void setProvidedBy(Type providedBy) {
        if (this.providedBy == null) {
            this.providedBy = providedBy;
        } else {
            throw new IllegalStateException("Can't set providedBy, already set.");
        }
    }
}
