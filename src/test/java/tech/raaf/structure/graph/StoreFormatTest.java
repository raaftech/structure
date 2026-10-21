package tech.raaf.structure.graph;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import tech.raaf.structure.schema.TypeSchemaTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

/**
 * Checks the stored format of some nodes from the bundles. MIGRATION.md
 * requires that this format does not change. The test ignores the SDN
 * artifacts that the migration removes: the "_" labels and "__type__".
 */
public class StoreFormatTest extends TypeSchemaTest {

    private static final String BUNDLES = "src/main/resources/bundles/";
    private static final String BASH = "6d467b28-449f-498c-9ca5-8660f522897f";
    private static final String CONTROLLER = "f482204e-7d63-45ca-96c9-10a7cc2783da";
    private static final String BASH_INVOKE = "8cabd118-dcc2-4bc6-bf0e-11cecfce4a5b";
    private static final String LINUX_PROFILE = "ce594183-8fcd-4723-8733-2e16242c91ec";

    private static final Set<String> FILE_KEYS = ImmutableSet.of("encoding", "format", "path", "content", "checksum");

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Test
    public void type() throws Exception {
        Stored n = read(BASH);
        assertEquals(ImmutableSet.of("Node", "Type", "type:Type", "type:Component", "type:Software",
                "type:Interpreter", "type:UnixShell", "type:Bash"), n.labels);
        assertEquals(ImmutableSet.of("uuid", "name", "desc", "bundle", "path", "checksum", "isAbstract",
                "file-dataSchema"), n.properties.keySet());
        assertEquals(BASH, n.properties.get("uuid"));
        assertEquals("Bash", n.properties.get("name"));
        assertEquals("The Bourne Again Shell.", n.properties.get("desc"));
        assertEquals("bash", n.properties.get("bundle"));
        assertEquals(BUNDLES + "bash/bash.type.json", n.properties.get("path"));
        assertEquals(false, n.properties.get("isAbstract"));
        assertFile(n, "file-dataSchema", "text", "json", "bash.schema.json");
        assertEquals(ImmutableSet.of("ISA -> 86168e4a-8c99-43c9-ad95-c8f88d96ac32"), n.outgoing);
    }

    @Test
    public void typeWithConstraint() throws Exception {
        Stored n = read(CONTROLLER);
        assertEquals(ImmutableSet.of("Node", "Type", "type:Type", "type:Instrument", "type:Controller"), n.labels);
        // SDN writes glyphId when it saves a type that has constraints
        assertEquals(ImmutableSet.of("uuid", "name", "desc", "bundle", "path", "checksum", "isAbstract",
                "glyphId", "file-dataSchema"), n.properties.keySet());
        assertEquals("Controller", n.properties.get("name"));
        assertEquals("core", n.properties.get("bundle"));
        assertEquals(BUNDLES + "core/types/instrument.type.json", n.properties.get("path"));
        assertEquals(false, n.properties.get("isAbstract"));
        assertEquals(0, n.properties.get("glyphId"));
        assertFile(n, "file-dataSchema", "text", "json", "../schema/controller.schema.json");
        assertEquals(ImmutableSet.of(
                "ISA -> 14dbfbfa-b50a-466f-8539-55d5d3f15e4a",
                "CONSTRAINTS -> f3498c02-58c6-486b-87c6-9d54607fdb12 {cardinality=OPTIONAL_TO_MANY, forRelationship=PRODUCED}"),
                n.outgoing);
    }

    @Test
    public void controller() throws Exception {
        Stored n = read(BASH_INVOKE);
        assertEquals(ImmutableSet.of("Node", "Instance", "Instrument", "Controller", "type:Type",
                "type:Instrument", "type:Controller"), n.labels);
        assertEquals(ImmutableSet.of("uuid", "name", "desc", "bundle", "path", "checksum", "data-subjectIsLocal",
                "data-instrumentType", "data-arguments", "file-dataSchema", "file-templateFile"),
                n.properties.keySet());
        assertEquals("BashInvoke", n.properties.get("name"));
        assertEquals("BashInvoke.", n.properties.get("desc"));
        assertEquals("bash", n.properties.get("bundle"));
        assertEquals(BUNDLES + "bash/bashscriptable.inst.json", n.properties.get("path"));
        assertEquals(true, n.properties.get("data-subjectIsLocal"));
        assertEquals("controller", n.properties.get("data-instrumentType"));
        assertJson("{\"script\": {\"value\": {\"from\": \"static\", \"type\": \"string\", \"strategy\": \"mustBeOne\"},"
                + " \"canBeUserSupplied\": false, \"isOptional\": false}}", n.properties.get("data-arguments"));
        assertFile(n, "file-dataSchema", "text", "json", "../core/schema/controller.schema.json");
        assertFile(n, "file-templateFile", "text", "hbs", "bashscriptable.hbs.sh");
        assertEquals(ImmutableSet.of(
                "INSTANCE_OF -> " + CONTROLLER,
                "PROVIDED_BY -> " + BASH,
                "APPLIES_TO -> " + BASH,
                "GIVES_CAPABILITY -> 4251040d-f05c-4711-8094-59ce773a8c35"),
                n.outgoing);
    }

    @Test
    public void profile() throws Exception {
        Stored n = read(LINUX_PROFILE);
        assertEquals(ImmutableSet.of("Node", "Instance", "Profile", "type:Type", "type:Profile"), n.labels);
        assertEquals(ImmutableSet.of("uuid", "name", "desc", "bundle", "path", "checksum", "data-commands"),
                n.properties.keySet());
        assertEquals("LinuxProfile", n.properties.get("name"));
        assertEquals("A profile that detects Linux.", n.properties.get("desc"));
        assertEquals("linux", n.properties.get("bundle"));
        assertEquals(BUNDLES + "linux/profiles/linux.inst.json", n.properties.get("path"));
        assertJson("{\"uname\": {\"command\": \"uname\", \"matches\": \"Linux\"}}", n.properties.get("data-commands"));
        assertEquals(ImmutableSet.of(
                "INSTANCE_OF -> c970f6d3-4898-4596-bfac-9d6d1f6b2541",
                "PROVIDED_BY -> a5613527-2f64-4481-83f2-80b0cb427e28",
                "APPLIES_TO -> a5613527-2f64-4481-83f2-80b0cb427e28"),
                n.outgoing);
    }

    private void assertJson(String expected, Object actual) throws Exception {
        assertEquals(mapper.readTree(expected), mapper.readTree((String) actual));
    }

    private void assertFile(Stored n, String key, String encoding, String format, String path) throws Exception {
        JsonNode file = mapper.readTree((String) n.properties.get(key));
        assertEquals(FILE_KEYS, Sets.newHashSet(file.fieldNames()));
        assertEquals(encoding, file.get("encoding").asText());
        assertEquals(format, file.get("format").asText());
        assertEquals(path, file.get("path").asText());
    }

    private Stored read(String uuid) {
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Node node = graphDatabaseService.findNodesByLabelAndProperty(DynamicLabel.label("Node"), "uuid", uuid)
                    .iterator().next();
            Stored stored = new Stored();
            for (Label label : node.getLabels()) {
                if (!label.name().startsWith("_")) {
                    stored.labels.add(label.name());
                }
            }
            for (String key : node.getPropertyKeys()) {
                stored.properties.put(key, node.getProperty(key));
            }
            for (Relationship r : node.getRelationships(Direction.OUTGOING)) {
                Map<String, Object> properties = new TreeMap<>();
                for (String key : r.getPropertyKeys()) {
                    if (!key.equals("__type__")) {
                        properties.put(key, r.getProperty(key));
                    }
                }
                stored.outgoing.add(r.getType().name() + " -> " + r.getEndNode().getProperty("uuid")
                        + (properties.isEmpty() ? "" : " " + properties));
            }
            tx.success();
            return stored;
        }
    }

    private static class Stored {
        final Set<String> labels = new HashSet<>();
        final Map<String, Object> properties = new HashMap<>();
        final Set<String> outgoing = new HashSet<>();
    }
}
