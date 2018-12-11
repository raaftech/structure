package tech.raaf.structure.graph.repositories;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.query.Param;
import tech.raaf.structure.graph.Controller;
import tech.raaf.structure.graph.Instrument;
import tech.raaf.structure.graph.Marker;
import tech.raaf.structure.graph.Type;

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
    @Query ("MATCH (commander:Group {uuid:{structureUuid}})<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()-[:MEMBER_OF]->(subject:Group{uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Service\"})," +
            "(client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(instrument)-[:GIVES_CAPABILITY]->({name:\"SendString\"})" +
            "RETURN DISTINCT instrument")
    Set<Controller> getCompatibleSendString(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    // Find the sendFile (client) controllers of the common protocols of an agent and subject
    @Query ("MATCH (commander:Group {uuid:{structureUuid}})<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF]->(client:Type)-[:ISA*0..]->(:Type)-[:IMPLEMENTS]->" +
            "protocol" +
            "<-[:IMPLEMENTS]-(:Type)<-[:ISA*0..]-(server:Type)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()-[:MEMBER_OF]->(subject:Group {uuid:{subjectUuid}})," +
            "(client)-[:ISA*0..]->({name:\"Client\"})," +
            "(server)-[:ISA*0..]->({name:\"Service\"})," +
            "(client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(instrument)-[:GIVES_CAPABILITY]->({name:\"SendFile\"})" +
            "RETURN DISTINCT instrument")
    Set<Controller> getCompatibleSendFile(@Param("structureUuid") String structure, @Param("subjectUuid") String subject);

    @Query ("MATCH (sendString {uuid:{sendStringInstrument}})-[:PROVIDED_BY]->(clientType),\n" +
            "(clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType),\n" +
            "(serverType)<-[:PROVIDED_BY]-(receiveString)-[:GIVES_CAPABILITY]->({name:\"ReceiveString\"})\n" +
            "RETURN DISTINCT receiveString")
    Marker getMatchingReceiveString(@Param("sendStringInstrument") String sendStringInstrumentUuid);

    @Query ("MATCH (sendFile {uuid:{sendFileInstrument}})-[:PROVIDED_BY]->(clientType),\n" +
            "(clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType),\n" +
            "(serverType)<-[:PROVIDED_BY]-(receiveFile)-[:GIVES_CAPABILITY]->({name:\"ReceiveFile\"})\n" +
            "RETURN DISTINCT receiveFile")
    Marker getMatchingReceiveFile(@Param("sendFileInstrument") String sendFileInstrumentUuid);

    // Get an invokable interpreter controller
    @Query ("MATCH (subject:Group {uuid:{instanceUuid}})<-[:MEMBER_OF]-()-[*0..]->(component:Instance)-[:ISA|INSTANCE_OF*..]->({name:\"Interpreter\"})," +
            "p=(component)-[:ISA|INSTANCE_OF*..]->(t)<-[:PROVIDED_BY]-(instrument)-[:GIVES_CAPABILITY]->({name:\"Invoke\"})" +
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
    @Query ("MATCH (structure:Group {uuid:{structureUuid}})<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF|ISA*..]->()<-[:PROVIDED_BY]-(instrument {uuid:{instrumentUuid}})," +
        "(instrument)-[:APPLIES_TO]->(patientType)<-[:INSTANCE_OF|ISA*..]-(patient {uuid:{patientUuid}})" +
            "RETURN instrument")
    Instrument validateNonLocalInstrument(@Param("instrumentUuid") String instrumentUuid, @Param("structureUuid") String structureUuid,
                                            @Param("patientUuid") String patientUuid);

    // Get the Type that is related to an instrument via PROVIDED_BY
    @Query ("MATCH (instrument {uuid:{instrumentUuid}})-[:PROVIDED_BY]->(instrumentOwner) " +
            "RETURN instrumentOwner.name")
    String findInstrumentOwner(@Param("instrumentUuid") String instrumentUuid);

    // When there are multiple hits for a controller name, we want to retrieve
    // the one that matches the origin.
    @Query ("MATCH (origin {uuid:{originUuid}})-[:INSTANCE_OF|ISA*1..]->(handled)<-[:APPLIES_TO]-(instrument{name:{instrumentName}})-[:PROVIDED_BY]->(provider), " +
            "       (startOfGroup {uuid:{startOfGroupUuid}})<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Controller findMatchingLocalController(@Param("originUuid") String originUuid,
                                           @Param("startOfGroupUuid") String startOfGroupUuid,
                                           @Param("instrumentName") String instrumentName);

    // Find all the locally run controllers that apply to a given origin
    @Query ("MATCH (origin{uuid:{originUuid}})-[:INSTANCE_OF|ISA*..]->(handled)" +
                    "<-[:APPLIES_TO]-(instrument)-[:PROVIDED_BY]->(enabler)" +
                "MATCH (origin:Instance)<-[*0..]-()-[:MEMBER_OF]->(grouping)" +
                "MATCH (grouping)<-[:MEMBER_OF]-()-[*1..]->(instance_of_enabler:Instance)-[:INSTANCE_OF|ISA*0..]->(enabler)" +
                    "<-[:PROVIDED_BY]-(instrument)" +
                "RETURN instrument")
    Set<Controller> findLocalControllers(@Param("originUuid") String originUuid);

    // Find all the remotely run controllers that apply to a given origin
    @Query ("MATCH (origin {uuid:{originUuid}})-[:INSTANCE_OF|ISA*1..]->(handled)<-[:APPLIES_TO]-(instrument)-[:PROVIDED_BY]->(provider), " +
            "       (structure:Group {uuid:{structureUuid}})<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF|ISA*1..]->(provider) " +
            "RETURN instrument")
    Set<Controller> findRemoteControllers(@Param("originUuid") String originUuid,
                                          @Param("structureUuid") String startOfGroupUuid);

    @Query("MATCH (n:Instrument) RETURN count(n)")
    long count();

    // Find a node's callable controllers
    @Query ("MATCH (:Instance {uuid: {0}})-[:INSTANCE_OF|ISA*0..]->()<-[:APPLIES_TO]-(controller:Controller)-[:GIVES_CAPABILITY]->(capability:`type:Capability` {name: {1}}) " +
            "RETURN controller")
    Set<Controller> findControllersPerCapability(String uuid, String name);

    @Query ("match (n{uuid:{0}})-[:INSTANCE_OF|ISA*..]->()<-[:APPLIES_TO]-()-[:GIVES_CAPABILITY]->(capability) return capability")
    Set<Type> findCapabilities(String uuid);
}
