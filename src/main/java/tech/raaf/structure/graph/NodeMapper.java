package tech.raaf.structure.graph;

import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Makes domain objects from the nodes that queries return.
 */
public final class NodeMapper {

    // The domain class of a node, by label, in this sequence
    static final Map<String, Supplier<Node>> CLASSES = ImmutableMap.<String, Supplier<Node>>builder()
            .put("Controller", Controller::new)
            .put("Marker", Marker::new)
            .put("Parser", Parser::new)
            .put("Component", Component::new)
            .put("Profile", Profile::new)
            .put("Result", Result::new)
            .put("Group", Group::new)
            .put("Type", Type::new)
            .build();

    private NodeMapper() {}

    @SuppressWarnings("unchecked")
    public static <T extends Node> T map(GraphNode graphNode) {
        if (graphNode == null) {
            return null;
        }
        final Node node = CLASSES.entrySet().stream()
                .filter(entry -> graphNode.getLabels().contains(entry.getKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No domain class for labels " + graphNode.getLabels()))
                .getValue().get();

        final Map<String, Object> properties = graphNode.getProperties();
        node.uuid = (String) properties.get("uuid");
        node.name = (String) properties.get("name");
        node.desc = (String) properties.get("desc");
        node.bundle = (String) properties.get("bundle");
        node.path = (String) properties.get("path");
        node.checksum = (String) properties.get("checksum");
        properties.forEach((key, value) -> {
            if (key.startsWith("data-")) {
                node.data.put(key.substring("data-".length()), value);
            } else if (key.startsWith("file-")) {
                node.file.put(key.substring("file-".length()), value);
            }
        });

        if (node instanceof Type) {
            final Type type = (Type) node;
            // The driver returns integers as Long
            final Object glyphId = properties.get("glyphId");
            type.glyphId = glyphId == null ? 0 : ((Number) glyphId).intValue();
            type.isAbstract = Boolean.TRUE.equals(properties.get("isAbstract"));
            final Object identity = properties.get("identity");
            type.identity = identity == null ? null : new LinkedHashSet<>((Collection<String>) identity);
            properties.forEach((key, value) -> {
                if (key.startsWith("kv-")) {
                    type.kv.put(key.substring("kv-".length()), value);
                }
            });
        }

        node.persisted = true;
        return (T) node;
    }

    // Map the nodes in one column of a list of rows
    public static <T extends Node> List<T> mapAll(List<Map<String, Object>> rows, String column) {
        final List<T> nodes = new ArrayList<>();
        rows.forEach(row -> nodes.add(map((GraphNode) row.get(column))));
        return nodes;
    }

    public static <T extends Node> Set<T> mapAllToSet(List<Map<String, Object>> rows, String column) {
        return new LinkedHashSet<>(mapAll(rows, column));
    }
}
