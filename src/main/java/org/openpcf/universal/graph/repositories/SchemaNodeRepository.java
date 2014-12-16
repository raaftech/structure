package org.openpcf.universal.graph.repositories;

import org.openpcf.universal.graph.SchemaNode;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * Give a description of SchemaNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-12
 */
public interface SchemaNodeRepository extends GraphRepository<SchemaNode> {

    SchemaNode findByUuid(String uuid);
    SchemaNode findByNameAndRevision(String name, int revision);
}
