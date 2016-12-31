package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Node;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.repository.query.Param;

import java.util.Set;

/**
 * Give a description of NodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public interface NodeRepository extends GenericRepository<Node> {

    @Query("MATCH (node:Node {uuid:{uuid}}) return node")
    org.neo4j.graphdb.Node findGraphNodeByUuid(@Param("uuid") String uuid);

    @Query("MATCH (node:Node {name: {name}}) return node")
    Set<Node> findByName(@Param("name") String name);

    @Query("MATCH (node:Node {uuid:{uuid}}) return node")
    Node findByUuid(@Param("uuid") String uuid);

    /**
     * Get all the ISA parents' names + a node's own name.
     * This is used to query all the names of a type so we know what it specializes.
     * @param uuid The uuid of the node.
     * @return The found names.
     */
    @Query("MATCH (t{uuid:{uuid}}) " +
            "WITH collect({id: id(t), name: t.name}) AS rows " +
            "MATCH (n{uuid:{uuid}})-[:ISA*0..]->(t) " +
            "WITH rows + collect({id: id(t), name: t.name}) AS allRows " +
            "UNWIND allRows AS row " +
            "WITH row ORDER BY row.id " +
            "RETURN DISTINCT row.name")
    Set<String> getTypeHierarchyForTypeUuid(@Param("uuid") String uuid);

    @QueryResult
    public class RelationshipTuple {
        public String name;
        public String adjacentUuid;
    }

    @Query("MATCH (entity{uuid:{uuid}})<-[r]-(adjacent) " +
            "RETURN type(r), adjacent.uuid")
    Set<RelationshipTuple> getIncomingRelationships(@Param("uuid") String uuid);

    @Query("MATCH (adjacent)<-[r]-(entity{uuid:{uuid}}) " +
            "RETURN type(r), adjacent.uuid")
    Set<RelationshipTuple> getOutgoingRelationships(@Param("uuid") String uuid);

}
