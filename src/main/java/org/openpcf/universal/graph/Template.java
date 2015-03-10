package org.openpcf.universal.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.neo4j.annotation.NodeEntity;

/**
 * Give a description of Template here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2015-03-09
 */
@NodeEntity
public interface Template {

    public ObjectNode getTemplateAsJson();
}
