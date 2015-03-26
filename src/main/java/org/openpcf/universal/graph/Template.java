package org.openpcf.universal.graph;

import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * Give a description of Template here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-24
 */
@NodeEntity
public interface Template {

    public String getUuid();
}
