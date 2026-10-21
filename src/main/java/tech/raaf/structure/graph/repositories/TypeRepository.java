package tech.raaf.structure.graph.repositories;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;
import tech.raaf.structure.graph.Constraint;
import tech.raaf.structure.graph.Type;

import java.util.List;
import java.util.Set;

/**
 * Give a description of TypeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
public interface TypeRepository extends GenericRepository<Type> {

    @Override
    @Query ("MATCH (node:Type {uuid:{0}}) return node")
    Type findByUuid(String uuid);

	@Override
    @Query ("MATCH (node:Type {name:{name}}) return node")
    Set<Type> findByName(@Param("name") String name);

	@Override
    @Query ("MATCH (node:Type {name:{name}}) return node LIMIT 1")
    Type findOneByName(@Param("name") String name);

    // Find a type by it's name, but case insensitive. This is more expensive than the case sensitive search.
    @Query ("MATCH (node:Type) WHERE LOWER(node.name) = LOWER({0}) RETURN node")
    Type findOneByCaseInsensitiveName(String name);

    // Get all types
    @Query ("MATCH (t:Type) return t")
    Set<Type> getAll();

    // Workaround, returning count(n) > 0 returns null when n is NULL.
    // This returns 0 if we're not of queried type, >0 if we are.
    @Query ("MATCH (start {uuid:{uuid}})-[:ISA*1..]->(n{name:{type}}) " +
            "RETURN count(n)")
    int isOfType(@Param("uuid") String uuid, @Param("type") String type);

    // This query finds common protocols between two (groups of) Instances.
    // This can be used to find ways to setup a connection between two computers.
    @Query ("MATCH (commander {uuid:{commanderUuid}})-[*0..]->(:Instance)-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "(protocol)" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-(:Instance)<-[*0..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})" +
            "RETURN protocol")
    Set<Type> getCommonProtocols(@Param("commanderUuid") String commander, @Param("subjectUuid") String subject);

    // Get the hierarchical path of a type
    @Query ("MATCH (deepest {uuid:{deepestNode}}) " +
            "OPTIONAL MATCH (deepest)-[:ISA*..]->(m) " +
            "WITH deepest, m ORDER BY id(m), deepest " +
            "WITH deepest, collect(m.name) AS names " +
            "WITH deepest, reduce(path='', n in names| path + n + '.') AS path " +
            "RETURN path + deepest.name AS path")
    String getHierarchyPath(@Param("deepestNode") String uuid);

    // Check whether a type is a subtype of another one. Every type
    // is a subtype of itself.
    // Workaround, returning count(n) > 0 returns null when n is NULL.
    @Query ("MATCH (start {uuid:{typeUuid}})<-[:ISA*0..]-(n{uuid:{subtypeUuid}}) " +
            "RETURN count(n)")
    int isSubtype(@Param("typeUuid") String typeUuid, @Param("subtypeUuid") String subtypeUuid);

    // Query to get a list of all Type glyphs
    @Query ("MATCH (n:Type) return n.glyphId")
    List<Integer> findAllTypeGlyphs();

    // Get a type's inherited outgoing constraints
    @Query("MATCH (n:Type {uuid: {0}})-[:ISA*1..]->()-[r:CONSTRAINTS]->() return r")
    Set<Constraint> getInheritedOutgoingConstraints(String typeUuid);

    // Get a type's inherited incoming constraints
    @Query("MATCH (n:Type {uuid: {0}})-[:ISA*1..]->()<-[r:CONSTRAINTS]-() return r")
    Set<Constraint> getInheritedIncomingConstraints(String typeUuid);

    // Check if one of the parents of a node has an identity property
    @Query("MATCH ({uuid: {0}})-[:ISA*..]->(p) " +
            "RETURN reduce(init = false, n IN COLLECT(p)| init OR n.identity IS NOT NULL) AS hasIdentity")
    boolean inheritsIdentity(String uuid);

    @Query("MATCH (n:Type) RETURN count(n)")
    long count();
}
