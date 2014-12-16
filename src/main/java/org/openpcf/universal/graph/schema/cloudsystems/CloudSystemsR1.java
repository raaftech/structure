package org.openpcf.universal.graph.schema.cloudsystems;

import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;

/**
 * Give a description of CloudSystemsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class CloudSystemsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("CloudSystemModel", "Main cloud (operating) system types.",
                "4a8e288a-696d-4616-bb77-970dcf3e150d", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected boolean createNodes() {
        // org.openpcf.model.cloudsystems-0.0.1
        createNode(new TypeNode("ApacheCloudStack", "The Apache CloudStack platform.", "c1d0eada-0d22-4026-aa2d-d865c1583482")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        createNode(new TypeNode("VmwareCloudFoundry", "The VMware CloudFoundry platform.",
                "6d080eef-d9c6-4be7-b81b-b9c57f0fe515")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        createNode(new TypeNode("Eucalyptus", "The Eucalyptus Cloud platform.", "87c66de2-e24b-4586-8170-5fcf6cc28424")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        createNode(new TypeNode("OpenNebula", "The OpenNebula Cloud platform.", "0e5b0e12-5975-43f5-9e92-5298a75a5f7a")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        createNode(new TypeNode("OpenShift", "The OpenShift Cloud platform.", "e57b8fbb-05a0-4c64-830a-8da17801e757")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        createNode(new TypeNode("OpenStack", "The OpenStack Cloud platform.", "55e9f170-9a98-42e3-ba58-f35c592eaeb0")
                .setIsa(typeNodeRepository.findByUuid("86c0439a-4219-46e6-82a3-6fb2ee774aa4")));
        return true;
    }
}
