package com.raaftech.structure.repository;

import com.raaftech.structure.graph.Node;
import com.raaftech.structure.graph.repositories.NodeRepository;

public class Nodes extends Repository<Node> {

    private NodeRepository repository;

    public Nodes(NodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
