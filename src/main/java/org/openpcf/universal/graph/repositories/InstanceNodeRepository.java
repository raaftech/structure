package org.openpcf.universal.graph.repositories;

import org.openpcf.universal.graph.InstanceNode;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * Give a description of InstanceNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-01-14
 */
public interface InstanceNodeRepository extends GraphRepository<InstanceNode> {

    InstanceNode findByUuid(String uuid);
}
