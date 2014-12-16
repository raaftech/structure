package org.openpcf.universal.graph.schema.operatingsystems;

import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;

/**
 * Give a description of OperatingSystemsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class OperatingSystemsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("OperatingSystemModel", "Main operating system types.",
                "90d2c69e-7199-45f4-bafa-5e0f848cf17d", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected boolean createNodes() {
        // org.openpcf.model.operatingsystems-0.0.1
        createNode(new TypeNode("DosLike", "A DOS-like operating system.", "e181e9e8-7f30-42a3-b87f-b8e183ddfd6f")
                .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
        createNode(new TypeNode("WindowsLike", "A Windows-like operating system.", "4edbaf6a-da58-4644-abac-ae80328ddb49")
                .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
        createNode(new TypeNode("Windows", "The Microsoft Windows Operating System.", "060a519d-08d0-4d12-82ac-1e3f14277009")
                .setIsa(typeNodeRepository.findByUuid("4edbaf6a-da58-4644-abac-ae80328ddb49")));
        createNode(new TypeNode("UnixLike", "A UNIX-like operating system.", "6a1eea8b-1462-4439-8c60-12edbee6c397")
                .setIsa(typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528")));
        createNode(new TypeNode("Unix", "The UNIX Operating System.", "a3b228b6-f737-460c-a3bc-bc653167b36c")
                .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));
        createNode(new TypeNode("Illumos", "The Illumos Operating System.", "f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")
                .setIsa(typeNodeRepository.findByUuid("a3b228b6-f737-460c-a3bc-bc653167b36c")));
        createNode(new TypeNode("MacOsX", "The Apple Mac OS X Operating System.", "0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")
                .setIsa(typeNodeRepository.findByUuid("a3b228b6-f737-460c-a3bc-bc653167b36c")));
        createNode(new TypeNode("Bsd", "The Berkeley Software Distribution Operating System.",
                "6004436f-7c88-4c2e-a29d-e8077bad10e9")
                .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));
        createNode(new TypeNode("Linux", "The Linux Operating System.", "a5613527-2f64-4481-83f2-80b0cb427e28")
                .setIsa(typeNodeRepository.findByUuid("6a1eea8b-1462-4439-8c60-12edbee6c397")));
        return true;
    }
}
