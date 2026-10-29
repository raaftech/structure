package tech.raaf.structure.graph.repositories;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import tech.raaf.structure.graph.Controller;
import tech.raaf.structure.graph.Graph;
import tech.raaf.structure.graph.Instrument;
import tech.raaf.structure.graph.Marker;
import tech.raaf.structure.graph.Type;

public class InstrumentRepository extends GenericRepository<Instrument> {

  @Override
  protected String label() {
    return "Instrument";
  }

  public Set<Instrument> findAllAsSet() {
    return set("MATCH (n:Instrument) RETURN n", ImmutableMap.of());
  }

  // Get the capability that a particular instrument implements
  public Type getInstrumentCapability(String uuid) {
    return one(
      "MATCH (start:Node {uuid: $uuid})-[:GIVES_CAPABILITY]->(n) RETURN n LIMIT 1",
      ImmutableMap.of("uuid", uuid)
    );
  }

  // Find the sendString (client) controllers of the common protocols of an agent and subject
  public Set<Controller> getCompatibleSendString(String structure, String subject) {
    return set(
      "MATCH (commander:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
        "-[:INSTANCE_OF]->(client)-[:ISA*0..]->()-[:IMPLEMENTS]->(protocol)" +
        "<-[:IMPLEMENTS]-()<-[:ISA*0..]-(server)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()" +
        "-[:MEMBER_OF]->(subject:Group {uuid: $subjectUuid}) " +
        "MATCH (client)-[:ISA*0..]->({name: 'Client'}) " +
        "MATCH (server)-[:ISA*0..]->({name: 'Service'}) " +
        "MATCH (client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'SendString'}) " +
        "RETURN DISTINCT n",
      ImmutableMap.of("structureUuid", structure, "subjectUuid", subject)
    );
  }

  // Find the sendFile (client) controllers of the common protocols of an agent and subject
  public Set<Controller> getCompatibleSendFile(String structure, String subject) {
    return set(
      "MATCH (commander:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
        "-[:INSTANCE_OF]->(client:Type)-[:ISA*0..]->(:Type)-[:IMPLEMENTS]->(protocol)" +
        "<-[:IMPLEMENTS]-(:Type)<-[:ISA*0..]-(server:Type)<-[:INSTANCE_OF]-(:Instance)<-[*..]-()" +
        "-[:MEMBER_OF]->(subject:Group {uuid: $subjectUuid}) " +
        "MATCH (client)-[:ISA*0..]->({name: 'Client'}) " +
        "MATCH (server)-[:ISA*0..]->({name: 'Service'}) " +
        "MATCH (client)-[:ISA*0..]->()<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'SendFile'}) " +
        "RETURN DISTINCT n",
      ImmutableMap.of("structureUuid", structure, "subjectUuid", subject)
    );
  }

  public Marker getMatchingReceiveString(String sendStringInstrumentUuid) {
    return one(
      "MATCH (sendString:Node {uuid: $uuid})-[:PROVIDED_BY]->(clientType) " +
        "MATCH (clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)" +
        "<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType) " +
        "MATCH (serverType)<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'ReceiveString'}) " +
        "RETURN DISTINCT n LIMIT 1",
      ImmutableMap.of("uuid", sendStringInstrumentUuid)
    );
  }

  public Marker getMatchingReceiveFile(String sendFileInstrumentUuid) {
    return one(
      "MATCH (sendFile:Node {uuid: $uuid})-[:PROVIDED_BY]->(clientType) " +
        "MATCH (clientType)-[:ISA*0..]->(clientParent)-[:IMPLEMENTS]->(protocol)" +
        "<-[:IMPLEMENTS]-(serverParent)<-[:ISA*0..]-(serverType) " +
        "MATCH (serverType)<-[:PROVIDED_BY]-(n)-[:GIVES_CAPABILITY]->({name: 'ReceiveFile'}) " +
        "RETURN DISTINCT n LIMIT 1",
      ImmutableMap.of("uuid", sendFileInstrumentUuid)
    );
  }

  // Get an invokable interpreter controller
  public Controller getInvokableInterpreter(String instanceUuid) {
    return one(
      "MATCH (subject:Group {uuid: $uuid})<-[:MEMBER_OF]-()-[*0..]->(component:Instance)" +
        "-[:ISA|INSTANCE_OF*..]->({name: 'Interpreter'}) " +
        "MATCH p = (component)-[:ISA|INSTANCE_OF*..]->(t)<-[:PROVIDED_BY]-(n)" +
        "-[:GIVES_CAPABILITY]->({name: 'Invoke'}) " +
        "RETURN n ORDER BY length(p) ASC, coalesce(n.`data-weight`, 0) DESC LIMIT 1",
      ImmutableMap.of("uuid", instanceUuid)
    );
  }

  // Used to purge all instruments from the database
  public void purgeInstruments() {
    Graph.query("MATCH (n:Instrument) DETACH DELETE n");
  }

  // This query checks if we can execute something about an origin, on the structure node.
  // A good example would be a ping check. Structure needs a particular tool (ping), that can do something
  // (ICMP echo request) which is about something that doesn't belong to its tree (the IP address of the origin).
  public Instrument validateNonLocalInstrument(String instrumentUuid, String structureUuid, String originUuid) {
    return one(
      "MATCH (structure:Group {uuid: $structureUuid})<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
        "-[:INSTANCE_OF|ISA*..]->()<-[:PROVIDED_BY]-(n:Node {uuid: $instrumentUuid}) " +
        "MATCH (n)-[:APPLIES_TO]->(originType)<-[:INSTANCE_OF|ISA*..]-(origin:Node {uuid: $originUuid}) " +
        "RETURN n LIMIT 1",
      ImmutableMap.of("instrumentUuid", instrumentUuid, "structureUuid", structureUuid, "originUuid", originUuid)
    );
  }

  // If the client of an instrument implements a protocol, the origin needs a server for it.
  // The server is part of what has the origin, for example the IcmpServer of an operating system
  // that has the IP address.
  public boolean originHasServer(String instrumentUuid, String originUuid) {
    return (
      one(
        "MATCH (n:Node {uuid: $instrumentUuid})-[:PROVIDED_BY]->(client), (origin:Node {uuid: $originUuid}) " +
          "WHERE NOT EXISTS { (client)-[:ISA*0..]->()-[:IMPLEMENTS]->() } " +
          "OR EXISTS { (client)-[:ISA*0..]->()-[:IMPLEMENTS]->(protocol)<-[:IMPLEMENTS]-()<-[:ISA*0..]-()" +
          "<-[:INSTANCE_OF]-(:Instance)<-[:HAS]-()-[:HAS*0..]->(origin) } " +
          "RETURN n LIMIT 1",
        ImmutableMap.of("instrumentUuid", instrumentUuid, "originUuid", originUuid)
      ) != null
    );
  }

  // Get the Type that is related to an instrument via PROVIDED_BY
  public String findInstrumentOwner(String instrumentUuid) {
    return value(
      "MATCH (instrument:Node {uuid: $uuid})-[:PROVIDED_BY]->(owner) RETURN owner.name AS n LIMIT 1",
      ImmutableMap.of("uuid", instrumentUuid)
    );
  }

  // Find the controllers that apply to an origin and can run
  public Set<Controller> findAvailableControllers(String originUuid, String structureUuid) {
    return findAvailableControllers(originUuid, structureUuid, null);
  }

  // Scripts use a provider on Structure and an interpreter on the subject.
  // Other local controllers use a provider on the subject.
  // A null capability matches each capability.
  public Set<Controller> findAvailableControllers(String originUuid, String structureUuid, String capability) {
    final Map<String, Object> params = new HashMap<>();
    params.put("originUuid", originUuid);
    params.put("structureUuid", structureUuid);
    params.put("capability", capability);
    return set(
      "MATCH (origin:Instance {uuid: $originUuid})-[:INSTANCE_OF|ISA*1..]->()<-[:APPLIES_TO]-(n:Controller) " +
        "WHERE $capability IS NULL OR EXISTS { (n)-[:GIVES_CAPABILITY]->(:`type:Capability` {name: $capability}) } " +
        "OPTIONAL MATCH (origin)<-[*0..]-()-[:MEMBER_OF]->(local:Group) " +
        "MATCH (structure:Group {uuid: $structureUuid}) " +
        "WITH DISTINCT n, local, CASE WHEN n.`data-subjectIsLocal` AND n.`data-interpreter` IS NULL " +
        "THEN local ELSE structure END AS home " +
        "WHERE EXISTS { (home)<-[:MEMBER_OF]-()-[*..]->(:Instance)" +
        "-[:INSTANCE_OF|ISA*..]->()<-[:PROVIDED_BY]-(n) } " +
        "AND (n.`data-interpreter` IS NULL OR EXISTS { " +
        "(local)<-[:MEMBER_OF]-()-[*..]->(:Instance)-[:INSTANCE_OF|ISA*..]->(interpreter:Type) " +
        "WHERE interpreter.name = n.`data-interpreter` }) " +
        "RETURN DISTINCT n",
      params
    );
  }

  public Set<Type> findCapabilities(String uuid) {
    return set(
      "MATCH (:Node {uuid: $uuid})-[:INSTANCE_OF|ISA*..]->()<-[:APPLIES_TO]-()" +
        "-[:GIVES_CAPABILITY]->(n) RETURN DISTINCT n",
      ImmutableMap.of("uuid", uuid)
    );
  }
}
