package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Controller;
import com.raaftech.structure.graph.Instrument;
import com.raaftech.structure.graph.Marker;
import com.raaftech.structure.graph.Type;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface InstrumentRepository extends GenericRepository<Instrument> {

    @Query("MATCH (n:Instrument) RETURN n")
    Set<Instrument> findAllAsSet();

    @Query("MATCH (n:Instrument{name:{instrumentName}}) RETURN n")
    @Override
    Set<Instrument> findByName(@Param("instrumentName") String instrumentName);

    @Query("MATCH (n:Instrument{name:{instrumentName}}) RETURN n LIMIT 1")
    Instrument findOneByName(@Param("instrumentName") String instrumentName);

    // Get the capability that a particular instrument implements
    @Query("MATCH (start {uuid:{uuid}})-[:GIVES_CAPABILITY]->(capability)" +
            "RETURN capability")
    Type getInstrumentCapability(@Param("uuid") String uuid);

    // Find the sendString (client) controllers of the common protocols of an agent and subject
    @Query ("MATCH (commander {uuid:{structureUuid}})-[:GROUPS|HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:GROUPS|HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})," +
            "(client)-[:ISA*0..]->()-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"SendString\"})" +
            "RETURN DISTINCT instrument")
    Set<Controller> getCompatibleSendString(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    // Find the sendFile (client) controllers of the common protocols of an agent and subject
    @Query ("MATCH (commander {uuid:{structureUuid}})-[:GROUPS|HAS_COMPONENT*..]->()-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-()<-[:GROUPS|HAS_COMPONENT*..]-(subject{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Server\"})," +
            "(client)-[:ISA*0..]->()-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"SendFile\"})" +
            "RETURN DISTINCT instrument")
    Set<Controller> getCompatibleSendFile(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    @Query ("MATCH (sendString {uuid:{sendStringInstrument}})<-[:HAS_INSTRUMENT]-(clientType),\n" +
            "(clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType),\n" +
            "(serverType)-[:HAS_INSTRUMENT]->(receiveString)-[:GIVES_CAPABILITY]->({name:\"ReceiveString\"})\n" +
            "RETURN DISTINCT receiveString")
    Marker getMatchingReceiveString(@Param("sendStringInstrument") String sendStringInstrumentUuid);

    @Query ("MATCH (sendFile {uuid:{sendFileInstrument}})<-[:HAS_INSTRUMENT]-(clientType),\n" +
            "(clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType),\n" +
            "(serverType)-[:HAS_INSTRUMENT]->(receiveFile)-[:GIVES_CAPABILITY]->({name:\"ReceiveFile\"})\n" +
            "RETURN DISTINCT receiveFile")
    Marker getMatchingReceiveFile(@Param("sendFileInstrument") String sendFileInstrumentUuid);

    // Get an invokable interpreter controller
    @Query ("MATCH (instance {uuid:{instanceUuid}})-[:GROUPS|HAS_COMPONENT*0..]->(component)-[:ISA|INSTANCE_OF*..]->({name:\"Interpreter\"})," +
            "p=(component)-[:ISA|INSTANCE_OF*..]->(t)-[:HAS_INSTRUMENT]->(instrument)-[:GIVES_CAPABILITY]->({name:\"Invoke\"})" +
            "RETURN instrument ORDER BY length(p) ASC LIMIT 1")
    Controller getInvokableInterpreter(@Param("instanceUuid") String instanceUuid);

    // Used to purge all instruments from the database
    @Query ("MATCH (n:Instrument) " +
            "OPTIONAL MATCH (n)<-[r]->(m) " +
            "DELETE r, n")
    void purgeInstruments();

    // This query checks if we can execute something about a patient, on the structure node.
    // A good example would be a ping check. Structure needs a particular tool (ping), that can do something (ICMP echo request)
    // which is about something that doesn't belong to its tree (the IP address of the patient).
    @Query ("MATCH (structure {uuid:{structureUuid}})-[:GROUPS|HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*..]->()-[:HAS_INSTRUMENT]->(instrument {uuid:{instrumentUuid}})," +
        "(instrument)-[:HANDLES]->(patientType)<-[:INSTANCE_OF|ISA*..]-(patient {uuid:{patientUuid}})" +
            "RETURN instrument")
    Instrument validateNonLocalInstrument(@Param("instrumentUuid") String instrumentUuid, @Param("structureUuid") String structureUuid,
                                            @Param("patientUuid") String patientUuid);

    // Get the Type that is related to an instrument via HAS_INSTRUMENT
    @Query ("MATCH (instrument {uuid:{instrumentUuid}})<-[:HAS_INSTRUMENT]-(instrumentOwner) " +
            "RETURN instrumentOwner.name")
    String findInstrumentOwner(@Param("instrumentUuid") String instrumentUuid);

    // When there are multiple hits for a controller name, we want to retrieve
    // the one that matches the origin.
    @Query ("MATCH (origin {uuid:{originUuid}})-[:INSTANCE_OF|ISA*1..]->(handled)<-[:HANDLES]-(instrument{name:{instrumentName}})<-[:HAS_INSTRUMENT]-(provider), " +
            "       (startOfGroup {uuid:{startOfGroupUuid}})-[:GROUPS|HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Controller findMatchingLocalController(@Param("originUuid") String originUuid,
                                           @Param("startOfGroupUuid") String startOfGroupUuid,
                                           @Param("instrumentName") String instrumentName);

    // Find all the locally run controllers that apply to a given origin
    @Query ("MATCH (origin{uuid:{originUuid}})-[:INSTANCE_OF|ISA*..]->(handled)" +
                    "<-[:HANDLES]-(instrument)<-[:HAS_INSTRUMENT]-(enabler)" +
                "MATCH (origin)<-[:HAS_COMPONENT*0..]-()<-[:GROUPS]-(grouping)" +
                "MATCH (grouping)-[:GROUPS|HAS_COMPONENT*1..]->(instance_of_enabler)-[:INSTANCE_OF|ISA*0..]->(enabler)" +
                    "-[:HAS_INSTRUMENT]->(instrument)" +
                "RETURN instrument")
    Set<Controller> findLocalControllers(@Param("originUuid") String originUuid);

    // Find all the remotely run controllers that apply to a given origin
    @Query ("MATCH (origin {uuid:{originUuid}})-[:INSTANCE_OF|ISA*1..]->(handled)<-[:HANDLES]-(instrument)<-[:HAS_INSTRUMENT]-(provider), " +
            "       (structure {uuid:{structureUuid}})-[:GROUPS|HAS_COMPONENT*..]->()-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Set<Controller> findRemoteControllers(@Param("originUuid") String originUuid,
                                          @Param("structureUuid") String startOfGroupUuid);

    @Query("MATCH (n:Instrument) RETURN count(n)")
    long count();
}
