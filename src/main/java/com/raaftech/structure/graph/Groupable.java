package com.raaftech.structure.graph;

import org.springframework.data.neo4j.annotation.NodeEntity;

import java.util.Set;

/**
 * Groupable is implemented by all graph persistable domain classes that can be
 * added to a group.
 */
@NodeEntity
public interface Groupable {

    /**
     * Returns the groups this groupable is member of.
     * @return See description.
     */
     Set<Group> getMemberOf();

    /**
     * Add a groupable entity to a group.
     * @param group The group to be added to.
     */
    default void addToGroup(Group group) {
        /*
        If we're already a member of this group return. This is done because we
        call `addMember` from `addToGroup` and `addToGroup` from `addMember`.
         */
        if (getMemberOf().contains(group)) {
            return;
        }

        getMemberOf().add(group);
        group.addMember(this);
    }
}
