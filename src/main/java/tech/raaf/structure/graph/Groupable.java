package tech.raaf.structure.graph;

import java.util.Set;

/**
 * Groupable is implemented by all graph persistable domain classes that can be
 * added to a group.
 */
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
        group.addMember(this);
    }
}
