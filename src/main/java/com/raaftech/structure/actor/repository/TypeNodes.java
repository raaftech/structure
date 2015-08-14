package com.raaftech.structure.actor.repository;

import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.repositories.TypeNodeRepository;

public class TypeNodes extends Repository<TypeNode> {

    private TypeNodeRepository repository;

    public TypeNodes(TypeNodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
