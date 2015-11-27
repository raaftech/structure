package com.raaftech.structure.graph.repositories;

import com.raaftech.structure.graph.Node;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

/**
 * Give a description of NodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
@Repository
public interface NodeRepository extends GenericRepository<Node> {
}
