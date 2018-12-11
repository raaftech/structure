package tech.raaf.structure.bus.repository;

import tech.raaf.structure.graph.Node;
import tech.raaf.structure.graph.repositories.NodeRepository;

public class Nodes extends Repository<Node> {

    private NodeRepository repository;

    public Nodes(NodeRepository repository) {
        super(repository);
        this.repository = repository;
    }

}
