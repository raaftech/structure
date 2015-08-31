package com.raaftech.structure.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

public class TypeNodes extends Repository<TypeNode> {

    private TypeNodeRepository repository;

    private Logger log = LoggerFactory.getLogger(TypeNodes.class);

    public TypeNodes(TypeNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
