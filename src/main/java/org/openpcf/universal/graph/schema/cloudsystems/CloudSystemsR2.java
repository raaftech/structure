package org.openpcf.universal.graph.schema.cloudsystems;

import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;

/**
 * Give a description of CloudSystemsR2 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-16
 */
public class CloudSystemsR2 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("CloudSystemModel", "Main cloud (operating) system types.",
                "d49a577a-0f19-49c3-b814-04a34843fe24", 2)
                .requires("BaseModel", 1);
    }

    @Override
    protected boolean createNodes() {
        createNode(new TypeNode("lol", "wtf"));
        return true;
    }
}
