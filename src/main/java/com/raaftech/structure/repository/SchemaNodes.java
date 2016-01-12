package com.raaftech.structure.repository;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.repositories.SchemaNodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaNodes extends Repository<SchemaNode> {

    private SchemaNodeRepository repository;

    private Logger log = LoggerFactory.getLogger(SchemaNodes.class);

    public SchemaNodes(SchemaNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
