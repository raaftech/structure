package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.TypeNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

/**
 * Give a description of TypeNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
public interface TypeNodeRepository extends GenericRepository<TypeNode> {

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node")
    Set<TypeNode> findByName(@Param("name") String name);

	@Override
    @Query ("START node=node:name(name={name}) MATCH (node:TypeNode) return node LIMIT 1")
    TypeNode findOneByName(@Param("name") String name);

    // Workaround, returning count(n) > 0 returns null when n is NULL.
    // This returns 0 if we're not of queried type, >0 if we are.
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:ISA*1..]->(n{name:{type}}) " +
            "RETURN count(n)")
    int isOfType(@Param("uuid") String uuid, @Param("type") String type);

    // This query finds common protocols between two (groups of) InstanceNodes.
    // This can be used to find ways to setup a connection between two computers.
    @Query ("START commander=node:uuid(uuid={commanderUuid}) " +
            "MATCH (commander)-[:HAS_COMPONENT*0..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:HAS_COMPONENT*0..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})" +
            "RETURN protocol")
    Set<TypeNode> getCommonProtocols(@Param("commanderUuid") String commander, @Param("subjectUuid") String subject);

    // Get the hierarchical path of a type
    @Query ("START deepest=node:uuid(uuid={deepestNode}) " +
            "MATCH (deepest) " +
            "OPTIONAL MATCH (deepest)-[:ISA*..]->(m) " +
            "WITH deepest, m ORDER BY id(m), deepest " +
            "WITH deepest, collect(m.name) AS names " +
            "WITH deepest, reduce(path='', n in names| path + n + '.') AS path " +
            "RETURN path + deepest.name AS path")
    String getHierarchyPath(@Param("deepestNode") String uuid);

    // Check whether a type is a subtype of another one. Every type
    // is a subtype of itself.
    // Workaround, returning count(n) > 0 returns null when n is NULL.
    @Query ("START start=node:uuid(uuid={typeUuid}) " +
            "MATCH start<-[:ISA*0..]-(n{uuid:{subtypeUuid}}) " +
            "RETURN count(n)")
    int isSubtype(@Param("typeUuid") String typeUuid, @Param("subtypeUuid") String subtypeUuid);

    // Development query: query all the typenodes and return their name + glyph
    // Find all instruments that apply to a given InstanceNode
    @QueryResult
    public class TypeQueryResult {
        @ResultColumn("name")
        public String name;
        @ResultColumn("glyphId")
        public String glyphId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getGlyphId() {
            return glyphId;
        }

        public void setGlyphId(String glyphId) {
            this.glyphId = Integer.toHexString(Integer.parseInt(glyphId));
        }
    }
    @Query ("MATCH (n:TypeNode)" +
            "RETURN n.name as name, n.glyphId as glyphId")
    List<TypeQueryResult> getNamesAndGlyphs();

}
