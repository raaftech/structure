package org.openpcf.universal.graph.repositories;

import org.openpcf.universal.graph.TemplateNode;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Set;

/**
 * Give a description of TemplateNodeRepository here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
public interface TemplateNodeRepository extends GraphRepository<TemplateNode> {

    TemplateNode findByUuid(String uuid);
    @Query("MATCH (n {startOfTemplate:true}) return n")
    Set<TemplateNode> findAllTemplates();
}
