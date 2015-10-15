package com.raaftech.structure.repository;

import java.util.Set;

import com.raaftech.structure.node.TemplateNodeActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TemplateNodeRepository;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

public class TemplateNodes extends Repository<TemplateNode> {

    private TemplateNodeRepository repository;

    private Logger log = LoggerFactory.getLogger(TemplateNodes.class);

    public TemplateNodes(TemplateNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Find all the templates.
     * @return a JSON object containing an array of all the separate templates.
     */
    public MessageHandler findAllTemplates() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                Set<TemplateNode> allTemplates = repository.findAllTemplates();
                ArrayNode templates = result.putArray("data");

                allTemplates.stream()
                .forEach(templateNode -> templates.add(templateNode.toJson()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Persist a TemplateNode to the database.
     * @return A JSON object containing the UUID of the new TemplateNode
     * or an error field if something went wrong.
     */
    @SchemaPath("schema/repository/templatenodes/putNode.schema.json")
    public MessageHandler putNode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                TypeNodeRepository typeNodeRepository = Context.INSTANCE.getTypeNodeRepository();
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                String parent = getArgs(args, "parent", (String)null);
                String name = args.get("name").asText();
                String desc = args.get("desc").asText();
                String isa = args.get("isa").asText();
                // We're using Boolean.parseBoolean, so this will evaluate to true only when the
                // string has value true (case ignored). Anything else will evaluate to false.
                boolean unique = Boolean.parseBoolean(getArgs(args, "unique", "false"));
                boolean required = Boolean.parseBoolean(getArgs(args, "required", "false"));
                boolean start = Boolean.parseBoolean(getArgs(args, "start", "false"));

                try {
                    if (parent != null && start == true) {
                        throw new RuntimeException("Cannot create TemplateNode that's both the start of a template and has a parent " +
                                "TemplateNode.");
                    }
                    if (parent == null && start == false) {
                        throw new RuntimeException("Cannot create TemplateNode that has no parent and isn't the start of a template.");
                    }

                    TypeNode isaTypeNode = typeNodeRepository.findByUuid(isa);
                    if (isaTypeNode != null) {
                        TemplateNode newTemplateNode = new TemplateNode(name, desc);
                        newTemplateNode.setIsa(isaTypeNode)
                        .setStartOfTemplate(start)
                        .setRequired(required)
                        .setUnique(unique);

                        if (parent != null) {
                            TemplateNode parentTemplateNode = repository.findByUuid(parent);
                            if (parentTemplateNode != null) {
                                parentTemplateNode.addComponent(newTemplateNode);
                                parentTemplateNode.save();
                                Context.INSTANCE.getVertx().deployVerticle(
                                        new TemplateNodeActor(newTemplateNode));
                            } else {
                                throw new RuntimeException("Cannot create TemplateNode, given parent was not found in the database.");
                            }
                        }
                        newTemplateNode.save();

                        result.put("data", newTemplateNode.getUuid());
                    } else {
                        throw new RuntimeException("Could not find TypeNode by which to group the Template.");
                    }
                } catch (RuntimeException e) {
                    log.error(e.getMessage());
                    result.put("error", e.getMessage());
                }

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

}
