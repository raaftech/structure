package org.openpcf.universal.graph.schema.servicemanagers;

import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;

/**
 * Give a description of ServiceManagersR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class ServiceManagersR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("ServiceManagerModel", "Main service manager types.",
                "4c305d8f-a9bb-4cfa-a512-4cc514c63349", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected boolean createNodes() {
        // org.openpcf.model.servicemanagers-0.0.1
        createNode(new TypeNode("SysvInit", "SysV Init Service Manager.", "608bfaf1-9edc-4609-b5cd-56d8aeff8174")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("BsdrcInit", "BSD RC Init Service Manager.", "b4c067de-e3cf-4b5b-90ca-588725d1b6c4")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("Upstart", "Upstart Service Manager.", "3aac3563-a493-4efa-a31b-e751ee7d0a6f")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("Systemd", "Systemd Service Manager.", "0c189a0e-6b78-4177-832d-5d9b35b8a6af")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("SystemStarter", "Mac OS X SystemStarter.", "fd13bd74-6682-4494-92af-a6caf5e64f12")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("Launchd", "Mac OS X Launchd.", "7b0bfefb-2212-4c58-a7c5-46ff66f0166a")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        createNode(new TypeNode("Scm", "Service Control Manager.", "198435e1-cf1d-4d0a-9fb8-c760b5e8ac2b")
                .setIsa(typeNodeRepository.findByUuid("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")));
        return true;
    }
}
