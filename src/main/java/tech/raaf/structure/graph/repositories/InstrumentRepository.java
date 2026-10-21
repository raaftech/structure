package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import tech.raaf.structure.graph.Controller;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Instrument;
import tech.raaf.structure.graph.Marker;
import tech.raaf.structure.graph.Type;

import java.util.Set;

public class InstrumentRepository extends GenericRepository<Instrument> {

    public InstrumentRepository() {
        super("Instrument");
    }

    public Set<Instrument> findAllAsSet() {
        return set("MATCH (n:Instrument) RETURN n", ImmutableMap.of());
    }

    // Get the capability that a particular instrument implements
    public Type getInstrumentCapability(String uuid) {
        return one("MATCH (start:Node {uuid: $uuid})-[:GIVES_CAPABILITY]->(n) RETURN n LIMIT 1",
                ImmutableMap.of("uuid", uuid));
    }

    // Find the sendString (client) controllers of the common protocols of an agent and subject
    public Set<Controller> getCompatibleSendString(String structure, String subject) {
        return set("MATCH (commander:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
                "-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->(protocol)" +
                "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()" +
                "-[:MEMBER_OF]->(subject:Group {uuid: $subjectUuid}) " +
                "MATCH (client)-[:ISA*0..]->({name: 'Client'}) " +
                "MATCH (server)-[:ISA*0..]->({name: 'Service'}) " +
                "MATCH (client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'SendString'}) " +
                "RETURN DISTINCT n", ImmutableMap.of("structureUuid", structure, "subjectUuid", subject));
    }

    // Find the sendFile (client) controllers of the common protocols of an agent and subject
    public Set<Controller> getCompatibleSendFile(String structure, String subject) {
        return set("MATCH (commander:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
                "-[:INSTANCE_OF]->(client:Type)-[:ISA*0..]->(:Type)-[:IMPLEMENTS]->(protocol)" +
                "<-[:IMPLEMENTS]-(:Type)<-[:ISA*0..]-(server:Type)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()" +
                "-[:MEMBER_OF]->(subject:Group {uuid: $subjectUuid}) " +
                "MATCH (client)-[:ISA*0..]->({name: 'Client'}) " +
                "MATCH (server)-[:ISA*0..]->({name: 'Service'}) " +
                "MATCH (client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'SendFile'}) " +
                "RETURN DISTINCT n", ImmutableMap.of("structureUuid", structure, "subjectUuid", subject));
    }

    public Marker getMatchingReceiveString(String sendStringInstrumentUuid) {
        return one("MATCH (sendString:Node {uuid: $uuid})-[:PROVIDED_BY]->(clientType) " +
                "MATCH (clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)" +
                "<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType) " +
                "MATCH (serverType)<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'ReceiveString'}) " +
                "RETURN DISTINCT n LIMIT 1", ImmutableMap.of("uuid", sendStringInstrumentUuid));
    }

    public Marker getMatchingReceiveFile(String sendFileInstrumentUuid) {
        return one("MATCH (sendFile:Node {uuid: $uuid})-[:PROVIDED_BY]->(clientType) " +
                "MATCH (clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)" +
                "<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType) " +
                "MATCH (serverType)<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'ReceiveFile'}) " +
                "RETURN DISTINCT n LIMIT 1", ImmutableMap.of("uuid", sendFileInstrumentUuid));
    }

    // Get an invokable interpreter controller
    public Controller getInvokableInterpreter(String instanceUuid) {
        return one("MATCH (subject:Group {uuid: $uuid})<-[:MEMBER_OF]-()-[*0..]->(component:Instance)" +
                "-[:ISA|INSTANCE_OF*..]->({name: 'Interpreter'}) " +
                "MATCH p = (component)-[:ISA|INSTANCE_OF*..]->(t)<-[:PROVIDED_BY]-(n)" +
                "-[:GIVES_CAPABILITY]->({name: 'Invoke'}) " +
                "RETURN n ORDER BY length(p) ASC LIMIT 1", ImmutableMap.of("uuid", instanceUuid));
    }

    // Used to purge all instruments from the database
    public void purgeInstruments() {
        Graph.query("MATCH (n:Instrument) DETACH DELETE n");
    }

    // This query checks if we can execute something about a patient, on the structure node.
    // A good example would be a ping check. Structure needs a particular tool (ping), that can do something (ICMP echo request)
    // which is about something that doesn't belong to its tree (the IP address of the patient).
    public Instrument validateNonLocalInstrument(String instrumentUuid, String structureUuid, String patientUuid) {
        return one("MATCH (structure:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
                "-[:INSTANCE_OF|ISA*..]->()<-[:PROVIDED_BY]-(n:Node {uuid: $instrumentUuid}) " +
                "MATCH (n)-[:APPLIES_TO]->(patientType)<-[:INSTANCE_OF|ISA*..]-(patient:Node {uuid: $patientUuid}) " +
                "RETURN n LIMIT 1", ImmutableMap.of("instrumentUuid", instrumentUuid,
                "structureUuid", structureUuid, "patientUuid", patientUuid));
    }

    // Get the Type that is related to an instrument via PROVIDED_BY
    public String findInstrumentOwner(String instrumentUuid) {
        return value("MATCH (instrument:Node {uuid: $uuid})-[:PROVIDED_BY]->(owner) RETURN owner.name AS n LIMIT 1",
                ImmutableMap.of("uuid", instrumentUuid));
    }

    // When there are multiple hits for a controller name, we want to retrieve
    // the one that matches the origin.
    public Controller findMatchingLocalController(String originUuid, String startOfGroupUuid, String instrumentName) {
        return one("MATCH (origin:Node {uuid: $originUuid})-[:INSTANCE_OF|ISA*1..]->(handled)" +
                "<-[:APPLIES_TO]-(n {name: $instrumentName})-[:PROVIDED_BY]->(provider) " +
                "MATCH (startOfGroup:Node {uuid: $startOfGroupUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
                "-[:INSTANCE_OF|ISA*1..]->(provider) " +
                "RETURN n LIMIT 1", ImmutableMap.of("originUuid", originUuid,
                "startOfGroupUuid", startOfGroupUuid, "instrumentName", instrumentName));
    }

    // Find all the locally run controllers that apply to a given origin
    public Set<Controller> findLocalControllers(String originUuid) {
        return set("MATCH (origin:Node {uuid: $originUuid})-[:INSTANCE_OF|ISA*..]->(handled)" +
                "<-[:APPLIES_TO]-(n)-[:PROVIDED_BY]->(enabler) " +
                "MATCH (origin:Instance)<-[*0..]-()-[:MEMBER_OF]->(grouping) " +
                "MATCH (grouping)<-[:MEMBER_OF]-()-[*1..]->(instance_of_enabler:Instance)" +
                "-[:INSTANCE_OF|ISA*0..]->(enabler)<-[:PROVIDED_BY]-(n) " +
                "RETURN DISTINCT n", ImmutableMap.of("originUuid", originUuid));
    }

    // Find all the remotely run controllers that apply to a given origin
    public Set<Controller> findRemoteControllers(String originUuid, String startOfGroupUuid) {
        return set("MATCH (origin:Node {uuid: $originUuid})-[:INSTANCE_OF|ISA*1..]->(handled)" +
                "<-[:APPLIES_TO]-(n)-[:PROVIDED_BY]->(provider) " +
                "MATCH (structure:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
                "-[:INSTANCE_OF|ISA*1..]->(provider) " +
                "RETURN DISTINCT n", ImmutableMap.of("originUuid", originUuid, "structureUuid", startOfGroupUuid));
    }

    // Find a node's callable controllers
    public Set<Controller> findControllersPerCapability(String uuid, String name) {
        return set("MATCH (:Instance {uuid: $uuid})-[:INSTANCE_OF|ISA*0..]->()<-[:APPLIES_TO]-(n:Controller)" +
                "-[:GIVES_CAPABILITY]->(capability:`type:Capability` {name: $name}) " +
                "RETURN DISTINCT n", ImmutableMap.of("uuid", uuid, "name", name));
    }

    public Set<Type> findCapabilities(String uuid) {
        return set("MATCH (:Node {uuid: $uuid})-[:INSTANCE_OF|ISA*..]->()<-[:APPLIES_TO]-()" +
                "-[:GIVES_CAPABILITY]->(n) RETURN DISTINCT n", ImmutableMap.of("uuid", uuid));
    }
}
