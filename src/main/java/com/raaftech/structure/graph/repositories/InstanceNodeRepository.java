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
 * Give a description of InstanceNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-14
 */
public interface InstanceNodeRepository extends GenericRepository<InstanceNode> {

    @Override
    @Query ("START node=node:name(name={name}) MATCH (node:InstanceNode) return node")
    Set<InstanceNode> findByName(@Param("name") String name);

    @Override
    @Query ("START node=node:name(name={name}) MATCH (node:InstanceNode) return node LIMIT 1")
    InstanceNode findOneByName(@Param("name") String name);

    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:HAS_COMPONENT*1..]->(n)-[:ISA|INSTANCE_OF*1..]->({name:{type}}) " +
            "RETURN n")
    Set<InstanceNode> findComponentsByType(@Param("uuid") String uuid, @Param("type") String type);

    // Workaround, returning count(n) > 0 returns null when n is NULL.
    // This returns 0 if we're not of queried type, >0 if we are.
    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH start-[:INSTANCE_OF|ISA*1..]->(n{name:{type}}) " +
            "RETURN count(n)")
    int isOfType(@Param("uuid") String uuid, @Param("type") String type);

    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH (start)-[:HAS_COMPONENT*0..]->()-[:ISA]->()-[:HAS_CAPABILITY]->({name:{type}})-[:HAS_HANDLER]->(n) " +
            "RETURN n")
    Set<InstanceNode> getCapabilities(@Param("uuid") String uuid, @Param("type") String capability);

    @Query ("START start=node:uuid(uuid={uuid}) " +
            "MATCH (start)-[:GIVES_CAPABILITY]->(capability)" +
            "RETURN capability")
    InstanceNode getHandlerCapability(@Param("uuid") String uuid);

    // Find the scriptable (client) handlers of the common protocols of a commander and subject
    @Query ("START commander=node:uuid(uuid={commanderUuid}) " +
            "MATCH (commander)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(client)-[:ISA*0..]->()-[:HAS_HANDLER]->(handler)-[:GIVES_CAPABILITY]->({name:\"Scriptable\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})" +
            "RETURN handler")
    Set<InstanceNode> getCompatibleScriptableHandlers(@Param("commanderUuid") String commander, @Param("subjectUuid") String subject);

    // Find the accessable (server) handlers of the common protocols of a commander and subject
    @Query ("START commander=node:uuid(uuid={commanderUuid}) " +
            "MATCH (commander)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})," +
            "(server)-[:ISA*0..]->()-[:HAS_HANDLER]->(handler)-[:GIVES_CAPABILITY]->({name:\"Accessible\"})" +
            "RETURN handler")
    Set<InstanceNode> getCompatibleAccessibleHandlers(@Param("commanderUuid") String commander, @Param("subjectUuid") String subject);

    // Get a handler of an InstanceNode only if we have the "handling" TypeNode in the expected tree as well.
    @Query ("START runOn=node:uuid(uuid={runOn}), handler=node:uuid(uuid={handlerUuid}), called=node:uuid(uuid={calledOn}) " +
            "MATCH (called)-[:INSTANCE_OF]->()-[:ISA*0..]->()<-[:HANDLES]-(handler)," +
            "(runOn)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF]->()-[:ISA*0..]->()-[:HAS_HANDLER]->(handler)" +
            "RETURN handler")
    InstanceNode getValidHandler(@Param("handlerUuid") String handlerUuid, @Param("calledOn") String calledOnUuid,
                                 @Param("runOn") String runOnGroupUuid);

    // Get the root node of a componental group
    @Query ("START instance=node:uuid(uuid={instanceUuid}) " +
            "MATCH (instance)<-[:HAS_COMPONENT*..]-(start) " +
            "WHERE NOT (start)<-[:HAS_COMPONENT]-() " +
            "RETURN start")
    InstanceNode getStartOfComponentGroup(@Param("instanceUuid") String instanceUuid);

    // Get a scriptable interpreter handler
    @Query ("START instance=node:uuid(uuid={instanceUuid}) " +
            "MATCH (instance)-[:HAS_COMPONENT*0..]->(component)-[:ISA|INSTANCE_OF*..]->({name:\"Interpreter\"})," +
            "p=(component)-[:ISA|INSTANCE_OF*..]->(t)-[:HAS_HANDLER]->(handler)-[:GIVES_CAPABILITY]->({name:\"Scriptable\"})" +
            "RETURN handler ORDER BY length(p) ASC LIMIT 1")
    InstanceNode getScriptableInterpreterHandler(@Param("instanceUuid") String instanceUuid);

    // Find all handlers that apply to a given InstanceNode
    @QueryResult
    public class HandlerQueryResult {
        @ResultColumn("limb")
        public InstanceNode limb;
        @ResultColumn("handler")
        public InstanceNode handler;
        @ResultColumn("capability")
        public TypeNode capability;

        public void setLimb(InstanceNode limb) {
            this.limb = limb;
        }

        public void setHandler(InstanceNode handler) {
            this.handler = handler;
        }

        public void setCapability(TypeNode capability) {
            this.capability = capability;
        }
    }

    @Query ("START patient=node:uuid(uuid={instanceUuid}), agent=node:uuid(uuid={structureUuid}) " +
            "MATCH (patient)-[:HAS_COMPONENT*..]->(limb)-[:INSTANCE_OF|ISA*..]->()<-[:HANDLES]-(handler)<-[HAS_HANDLER]-(owner)," +
            "(agent)," +
            "(handler)-[:GIVES_CAPABILITY]->(capability)" +
            "WHERE ((handler{`kv-subjectIsLocal`:\"true\"})<-[:HAS_HANDLER]-(owner)" +
            "   AND (patient)-[:HAS_COMPONENT*0..]->()-[:INSTANCE_OF|ISA*..]->(owner))" +
            "OR ((handler{`kv-subjectIsLocal`:\"false\"})<-[:HAS_HANDLER]-(owner)" +
            "   AND (agent)-[:HAS_COMPONENT*0..]->()-[:INSTANCE_OF|ISA*..]->(owner))" +
            "RETURN limb, handler, capability ORDER BY id(limb)")
    List<HandlerQueryResult> getHandlers(@Param("instanceUuid") String instanceUuid, @Param("structureUuid") String structureUuid);

    // Used to purge all handlers from the database
    @Query ("MATCH ()-[r1:HAS_HANDLER]->(handler)-[r2:GIVES_CAPABILITY]->()," +
            "      (handler)-[r3:INSTANCE_OF]->(:TypeNode{name:\"Handler\"})," +
            "      (handler)-[r4:HANDLES]->(:TypeNode)" +
            "delete r1,r2,r3,r4,handler;")
    void purgeHandlers();

    @Query ("MATCH (n:InstanceNode) WHERE NOT (n)-[:INSTANCE_OF]->({name:\"Handler\"}) RETURN n ORDER BY id(n) DESC LIMIT {amount}")
    List<InstanceNode> getNLastCreated(@Param("amount") int amount);
}
