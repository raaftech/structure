package com.raaftech.structure.helper.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.Constraint;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.helper.LambdaHelper;
import com.raaftech.structure.next.Entity;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class BundleLoader {

    final private List<JsonNode> sortedNodes;
    final private List<Entity> typeEntities;
    final private List<Entity> nonTypeEntities;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final TypeRepository typeRepository = Context.getTypeRepository();

    public BundleLoader() throws Exception {
        EvenNaiverSorter evenNaiverSorter = new EvenNaiverSorter();
        sortedNodes = evenNaiverSorter.getUpdateList(evenNaiverSorter.sortIt(evenNaiverSorter.readAllJson()));
        typeEntities = getSortedTypes().stream()
                .map(json -> LambdaHelper.wrapWithExceptionPropagation(() -> mapper.treeToValue(json, Entity.class)).get())
                .collect(Collectors.toList());
        nonTypeEntities = getSortedInstances().stream()
                .map(json -> LambdaHelper.wrapWithExceptionPropagation(() -> mapper.treeToValue(json, Entity.class)).get())
                .collect(Collectors.toList());
    }

    private List<JsonNode> getSortedTypes() {
        return sortedNodes.stream()
                .filter(json -> Objects.equals("Type", json.get("type").asText()))
                .collect(Collectors.toList());
    }

    private List<JsonNode> getSortedInstances() {
        return sortedNodes.stream()
                .filter(json -> !Objects.equals("Type", json.get("type").asText()))
                .collect(Collectors.toList());
    }

    private void persistTypeConstraints(Entity typeEntity) {
        final Type type = typeRepository.findByUuid(typeEntity.getUuid());
        Validate.notNull(type, "Can't find type with UUID " + typeEntity.getUuid());

        // We might not have constraints for this type
        if (!typeEntity.getConstraints().isPresent()) {
            return;
        }

        Iterator<Map.Entry<String, List<Pair<Constraint.Cardinality, UUID>>>> constraintIter =
                typeEntity.getConstraints().get().entrySet().iterator();

        while(constraintIter.hasNext()) {
            Map.Entry<String, List<Pair<Constraint.Cardinality, UUID>>> entry =  constraintIter.next();
            final String relName = entry.getKey();

            entry.getValue().stream()
                    .forEach(constraintData -> type.addConstraint(
                            typeRepository.findByUuid(constraintData.getRight().toString()), constraintData.getLeft(), relName));
        }

        type.save();
    }

    public void load() {
        // Persist the types first
        typeEntities.forEach(Entity::toGraph);

        /*
        We'll now do a second pass on the types, but we'll get them from the graph
        so they're actual instances of Type on which we can create constraints.
         */
        typeEntities.forEach(this::persistTypeConstraints);
    }
}
