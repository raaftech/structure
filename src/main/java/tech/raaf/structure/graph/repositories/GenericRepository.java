package tech.raaf.structure.graph.repositories;

import org.springframework.data.neo4j.repository.GraphRepository;
import tech.raaf.structure.graph.Node;

import java.util.Set;

public interface GenericRepository<T extends Node> extends GraphRepository<T> {

    T findByUuid(String uuid);
    Set<T> findByName(String name);
    T findOneByName(String name);
}
