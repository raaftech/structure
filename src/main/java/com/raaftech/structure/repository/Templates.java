package com.raaftech.structure.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.raaftech.structure.annotations.SchemaPath;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.graph.Template;
import com.raaftech.structure.graph.Type;
import com.raaftech.structure.graph.repositories.TemplateRepository;
import com.raaftech.structure.graph.repositories.TypeRepository;
import com.raaftech.structure.node.TemplateActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Templates extends Repository<Template> {

    private TemplateRepository repository;

    private Logger log = LoggerFactory.getLogger(Templates.class);

    public Templates(TemplateRepository repository) {
        super(repository);
        this.repository = repository;
    }

    /**
     * Find all the templates.
     * @return a JSON object containing an array of all the separate templates.
     */
    @SuppressWarnings("unused")
    public MessageHandler findAllTemplates() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                ObjectNode result = mapper.createObjectNode();
                List<Template> allTemplates = repository.findAllTemplates();
                ArrayNode templates = result.putArray("data");

                allTemplates.stream()
                        .forEach(template -> templates.add(template.toJson()));

                return response(request, result);
            }

            @Override
            public JsonNode help(JsonNode request) {
                return null;
            }
        };
    }

    /**
     * Persist a Template to the database.
     * @return A JSON object containing the UUID of the new Template
     * or an error field if something went wrong.
     */
    @SchemaPath("schema/repository/templates/putNode.schema.json")
    @SuppressWarnings("unused")
    public MessageHandler putNode() {
        return new MessageHandler() {
            @Override
            public JsonNode exec(JsonNode request) {
                TypeRepository typeRepository = Context.getTypeRepository();
                ObjectNode result = mapper.createObjectNode();
                JsonNode args = request.get("args");

                String parent = null;
                if (args.get("parent") != null) {
                    parent = args.get("parent").asText();
                }
                String name = args.get("name").asText();
                String desc = args.get("desc").asText();
                String isa = args.get("isa").asText();
                // We're using Boolean.parseBoolean, so this will evaluate to true only when the
                // string has value true (case ignored). Anything else will evaluate to false.
                boolean unique = Boolean.parseBoolean(getArgs(args, "unique", "false"));
                boolean required = Boolean.parseBoolean(getArgs(args, "required", "false"));
                boolean start = Boolean.parseBoolean(getArgs(args, "start", "false"));

                try {
                    if (parent != null && start) {
                        throw new RuntimeException("Cannot create Template that's both the start of a template and has a parent " +
                                "Template.");
                    }
                    if (parent == null && !start) {
                        throw new RuntimeException("Cannot create Template that has no parent and isn't the start of a template.");
                    }

                    Type isaType = typeRepository.findByUuid(isa);
                    if (isaType != null) {
                        Template newTemplate = new Template(name, desc);
                        newTemplate.setIsa(isaType)
                        .setStartOfTemplate(start)
                        .setRequired(required)
                        .setUnique(unique);

                        if (parent != null) {
                            Template parentTemplate = repository.findByUuid(parent);
                            if (parentTemplate != null) {
                                parentTemplate.addComponent(newTemplate);
                                parentTemplate.save();
                                Context.getVertx().deployVerticle(
                                        new TemplateActor(newTemplate));
                            } else {
                                throw new RuntimeException("Cannot create Template, given parent was not found in the database.");
                            }
                        }
                        newTemplate.save();

                        result.put("data", newTemplate.getUuid());
                    } else {
                        throw new RuntimeException("Could not find Type by which to group the Template.");
                    }
                } catch (RuntimeException e) {
                	log.error(e.getMessage());

        			if (Boolean.parseBoolean(System.getProperty("traceMode"))) {
        				e.printStackTrace();
        			}
        			
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
