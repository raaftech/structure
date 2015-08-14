package com.raaftech.structure.actor.repository;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.repositories.GenericRepository;
import com.raaftech.structure.graph.repositories.SchemaNodeRepository;

public class SchemaNodes extends Repository<SchemaNode> {

    private SchemaNodeRepository repository;

    public SchemaNodes(SchemaNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
