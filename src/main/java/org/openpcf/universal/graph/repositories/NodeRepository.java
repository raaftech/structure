package org.openpcf.universal.graph.repositories;

import org.openpcf.universal.graph.Node;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * Give a description of NodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-26
 */
public interface NodeRepository extends GraphRepository<Node> {

    Node findByUuid(String uuid);
}
