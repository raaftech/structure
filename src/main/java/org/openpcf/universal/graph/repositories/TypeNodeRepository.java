package org.openpcf.universal.graph.repositories;

import org.openpcf.universal.graph.TypeNode;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * Give a description of TypeNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-09
 */
public interface TypeNodeRepository extends GraphRepository<TypeNode> {

    TypeNode findByUuid(String uuid);
    TypeNode findByName(String name);
}
