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
            "MATCH (start)-[:GIVES_CAPABILITY]->(capability)" +
            "RETURN capability")
    InstanceNode getInstrumentCapability(@Param("uuid") String uuid);

    // Find the sendString (client) instruments of the common protocols of an agent and subject
    @Query ("START commander=node:uuid(uuid={structureUuid}) " +
            "MATCH (commander)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})," +
            "(client)-[:ISA*0..]->()-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"SendString\"})" +
            "RETURN DISTINCT instrument")
    Set<InstanceNode> getCompatibleSendString(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    // Find the sendFile (client) instruments of the common protocols of an agent and subject
    @Query ("START commander=node:uuid(uuid={structureUuid}) " +
            "MATCH (commander)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})," +
            "(client)-[:ISA*0..]->()-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"SendFile\"})" +
            "RETURN DISTINCT instrument")
    Set<InstanceNode> getCompatibleSendFile(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    @Query ("START sendString=node:uuid(uuid={sendStringInstrument}) " +
            "MATCH (sendString)<-[:HAS_INSTRUMENT]-(clientType),\n" +
            "(clientType)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverType),\n" +
            "(serverType)-[:HAS_INSTRUMENT]->(receiveString)-[:GIVES_CAPABILITY]->({name:\"ReceiveString\"})\n" +
            "RETURN DISTINCT receiveString")
    InstanceNode getMatchingReceiveString(@Param("sendStringInstrument") String sendStringInstrumentUuid);

    @Query ("START sendFile=node:uuid(uuid={sendFileInstrument}) " +
            "MATCH (sendFile)<-[:HAS_INSTRUMENT]-(clientType),\n" +
            "(clientType)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverType),\n" +
            "(serverType)-[:HAS_INSTRUMENT]->(receiveFile)-[:GIVES_CAPABILITY]->({name:\"ReceiveFile\"})\n" +
            "RETURN DISTINCT receiveFile")
    InstanceNode getMatchingReceiveFile(@Param("sendFileInstrument") String sendFileInstrumentUuid);

    // Get the root node of a componental group
    @Query ("START instance=node:uuid(uuid={instanceUuid}) " +
            "MATCH (instance)<-[:HAS_COMPONENT*0..]-(start) " +
            "WHERE NOT (start)<-[:HAS_COMPONENT]-() " +
            "RETURN start")
    InstanceNode getStartOfComponentGroup(@Param("instanceUuid") String instanceUuid);

    // Get an invokable interpreter instrument
    @Query ("START instance=node:uuid(uuid={instanceUuid}) " +
            "MATCH (instance)-[:HAS_COMPONENT*0..]->(component)-[:ISA|INSTANCE_OF*..]->({name:\"Interpreter\"})," +
            "p=(component)-[:ISA|INSTANCE_OF*..]->(t)-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"Invoke\"})" +
            "RETURN instrument ORDER BY length(p) ASC LIMIT 1")
    InstanceNode getInvokableInterpreter(@Param("instanceUuid") String instanceUuid);

    // Used to purge all instruments from the database
    @Query ("MATCH ()-[r1:HAS_INSTRUMENT]->(instrument)-[r2:GIVES_CAPABILITY]->()," +
            "      (instrument)-[r3:INSTANCE_OF]->(:TypeNode{name:\"Instrument\"})," +
            "      (instrument)-[r4:HANDLES]->(:TypeNode)" +
            "delete r1,r2,r3,r4,instrument;")
    void purgeHandlers();

    @Query ("MATCH (n:InstanceNode) WHERE NOT (n)-[:INSTANCE_OF]->({name:\"Handler\"}) RETURN n ORDER BY id(n) DESC LIMIT {amount}")
    List<InstanceNode> getNLastCreated(@Param("amount") int amount);

    // This query checks if we can execute something about a patient, on the structure node.
    // A good example would be a ping check. Structure needs a particular tool (ping), that can do something (ICMP echo request)
    // which is about something that doesn't belong to its tree (the IP address of the patient).
    @Query ("START structure=node:uuid(uuid={structureUuid}), instrument=node:uuid(uuid={instrumentUuid}), patient=node:uuid(uuid={patientUuid}) " +
            "MATCH (structure)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*..]->()-[:HAS_INSTRUMENT]->(instrument)," +
            "(instrument)-[:HANDLES]->(patientType)<-[:INSTANCE_OF|ISA*..]-(patient)" +
            "RETURN instrument")
    InstanceNode validateNonLocalInstrument(@Param("instrumentUuid") String instrumentUuid, @Param("structureUuid") String structureUuid,
                                            @Param("patientUuid") String patientUuid);

    // Get the TypeNode that is related to an instrument via HAS_INSTRUMENT
    @Query ("START instrument=node:uuid(uuid={instrumentUuid}) " +
            "MATCH (instrument)<-[:HAS_INSTRUMENT]-(instrumentOwner) " +
            "RETURN instrumentOwner.name")
    String findInstrumentOwner(@Param("instrumentUuid") String instrumentUuid);

    // When there are multiple hits for an instrument name, we want to retrieve
    // the one that matches the origin.
    @Query ("START origin=node:uuid(uuid={originUuid}), "+
            "      startOfGroup=node:uuid(uuid={startOfGroupUuid}) " +
            "MATCH (origin)-[:INSTANCE_OF|ISA*1..]->(handled)<-[:HANDLES]-(instrument{name:{instrumentName}})<-[:HAS_INSTRUMENT]-(provider), " +
            "       (startOfGroup)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    InstanceNode findMatchingLocalInstrument(@Param("originUuid") String originUuid,
                                        @Param("startOfGroupUuid") String startOfGroupUuid,
                                        @Param("instrumentName") String instrumentName);

    // Find all the locally run instruments that apply to a given origin
    @Query ("START origin=node:uuid(uuid={originUuid}), "+
            "      startOfGroup=node:uuid(uuid={startOfGroupUuid}) " +
            "MATCH (origin)-[:INSTANCE_OF|ISA*1..]->(handled)<-[:HANDLES]-(instrument)<-[:HAS_INSTRUMENT]-(provider), " +
            "       (startOfGroup)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Set<InstanceNode> findLocalInstruments(@Param("originUuid") String originUuid,
                                             @Param("startOfGroupUuid") String startOfGroupUuid);

    // Find all the remotely run instruments that apply to a given origin
    @Query ("START origin=node:uuid(uuid={originUuid}), "+
            "      structure=node:uuid(uuid={structureUuid}) " +
            "MATCH (origin)-[:INSTANCE_OF|ISA*1..]->(handled)<-[:HANDLES]-(instrument)<-[:HAS_INSTRUMENT]-(provider), " +
            "       (structure)-[:HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Set<InstanceNode> findRemoteInstruments(@Param("originUuid") String originUuid,
                                      @Param("structureUuid") String startOfGroupUuid);

    // Get all the InstanceNodes we need for our demo
    @Query ("MATCH (n:InstanceNode)-[:INSTANCE_OF]->()-[:ISA*..]->(:TypeNode{name:\"OperatingSystem\"}) " +
            "RETURN n")
    List<InstanceNode> getRaafMachines();

    // Get all the http daemons for the demo
    @Query ("MATCH (:TypeNode{name:\"HttpServer\"})<-[:INSTANCE_OF]-(httpd) " +
            "RETURN httpd")
    Set<InstanceNode> getHttpServers();

}
