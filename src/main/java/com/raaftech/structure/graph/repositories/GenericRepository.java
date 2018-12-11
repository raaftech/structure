package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Node;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

public interface GenericRepository<T extends Node> extends GraphRepository<T> {

    T findByUuid(String uuid);
    Set<T> findByName(String name);
    T findOneByName(String name);
}
