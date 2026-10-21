package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Constraint;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Give a description of TypeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
public class TypeRepository extends GenericRepository<Type> {

    public TypeRepository() {
        super("Type");
    }

    // Find a type by it's name, but case insensitive. This is more expensive than the case sensitive search.
    public Type findOneByCaseInsensitiveName(String name) {
        return one("MATCH (n:Type) WHERE toLower(n.name) = toLower($name) RETURN n LIMIT 1",
                ImmutableMap.of("name", name));
    }

    // Get all types
    public Set<Type> getAll() {
        return set("MATCH (n:Type) RETURN n", ImmutableMap.of());
    }

    // This returns 0 if we're not of queried type, >0 if we are.
    public int isOfType(String uuid, String type) {
        return (int) number("MATCH (start:Node {uuid: $uuid})-[:ISA*1..]->(n {name: $type}) " +
                "RETURN count(n) AS n", ImmutableMap.of("uuid", uuid, "type", type));
    }

    // This query finds common protocols between two (groups of) Instances.
    // This can be used to find ways to setup a connection between two computers.
    public Set<Type> getCommonProtocols(String commander, String subject) {
        return set("MATCH (commander:Node {uuid: $commanderUuid})-[*0..]->(:Instance)-[:INSTANCE_OF]->(client)" +
                "-[:ISA*0..]->()-[:IMPLEMENTS]->(n)<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)" +
                "<-[:INSTANCE_OF]-(:Instance)<-[*0..]-(subject:Node {uuid: $subjectUuid}) " +
                "MATCH (client)-[:ISA*0..]->({name: 'Client'}) " +
                "MATCH (server)-[:ISA*0..]->({name: 'Server'}) " +
                "RETURN DISTINCT n", ImmutableMap.of("commanderUuid", commander, "subjectUuid", subject));
    }

    // Get the hierarchical path of a type: the names from the root type down, joined with dots
    public String getHierarchyPath(String uuid) {
        return value("MATCH p = (t:Type {uuid: $uuid})-[:ISA*0..]->(root:Type) " +
                "WHERE NOT EXISTS { (root)-[:ISA]->() } " +
                "RETURN reduce(s = '', n IN reverse(nodes(p)) | " +
                "s + CASE WHEN s = '' THEN '' ELSE '.' END + n.name) AS n LIMIT 1",
                ImmutableMap.of("uuid", uuid));
    }

    // Check whether a type is a subtype of another one. Every type
    // is a subtype of itself.
    public int isSubtype(String typeUuid, String subtypeUuid) {
        return (int) number("MATCH (start:Node {uuid: $typeUuid})<-[:ISA*0..]-(n:Node {uuid: $subtypeUuid}) " +
                "RETURN count(n) AS n", ImmutableMap.of("typeUuid", typeUuid, "subtypeUuid", subtypeUuid));
    }

    // Query to get a list of all Type glyphs; the driver returns Long
    public List<Integer> findAllTypeGlyphs() {
        final List<Integer> glyphs = new ArrayList<>();
        Graph.query("MATCH (n:Type) RETURN n.glyphId AS glyphId").forEach(row -> {
            final Number glyphId = (Number) row.get("glyphId");
            glyphs.add(glyphId == null ? null : glyphId.intValue());
        });
        return glyphs;
    }

    // Get a type's inherited outgoing constraints
    public Set<Constraint> getInheritedOutgoingConstraints(String typeUuid) {
        return Constraint.fromRows(Graph.query("MATCH (:Type {uuid: $uuid})-[:ISA*1..]->(a)-[r:CONSTRAINTS]->(b) " +
                "RETURN a, b, r.forRelationship AS forRelationship, r.cardinality AS cardinality",
                ImmutableMap.of("uuid", typeUuid)));
    }

    // Get a type's inherited incoming constraints
    public Set<Constraint> getInheritedIncomingConstraints(String typeUuid) {
        return Constraint.fromRows(Graph.query("MATCH (:Type {uuid: $uuid})-[:ISA*1..]->(b)<-[r:CONSTRAINTS]-(a) " +
                "RETURN a, b, r.forRelationship AS forRelationship, r.cardinality AS cardinality",
                ImmutableMap.of("uuid", typeUuid)));
    }

    // Check if one of the parents of a node has an identity property
    public boolean inheritsIdentity(String uuid) {
        return Boolean.TRUE.equals(value("MATCH (:Node {uuid: $uuid})-[:ISA*..]->(p) " +
                "RETURN reduce(init = false, n IN collect(p) | init OR n.identity IS NOT NULL) AS n",
                ImmutableMap.of("uuid", uuid)));
    }
}
