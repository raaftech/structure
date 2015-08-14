package com.raaftech.structure.graph.repositories;

import java.util.Set;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.raaftech.structure.graph.Node;

public interface GenericRepository<T extends Node> extends GraphRepository<T> {

    public T findByUuid(String uuid);
    public Set<T> findByName(String name);
    public T findOneByName(String name);
}
