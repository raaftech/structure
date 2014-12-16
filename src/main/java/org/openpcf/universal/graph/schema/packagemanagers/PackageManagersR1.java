package org.openpcf.universal.graph.schema.packagemanagers;

import org.openpcf.universal.graph.SchemaNode;
import org.openpcf.universal.graph.TypeNode;
import org.openpcf.universal.graph.schema.SchemaUpdate;

/**
 * Give a description of PackageManagersR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class PackageManagersR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("PackageManagerModel", "Main package manager types.",
                "88846580-7ca5-4d7c-aba4-f350e61dd5ce", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected boolean createNodes() {
        // org.openpcf.model.packagemanagers-0.0.1
        createNode(new TypeNode("PacMan", "Arch Linux Pacman Package Manager.", "ad047d85-792a-4385-a14c-b85266bc5176")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("BsdPkg", "BSD pkg Package Manager.", "95d6093b-448b-4a62-8642-dc8d32b43622")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("BsdPkgNg", "BSD pkgng Package Manager.", "1a879371-f680-4514-8189-5d6e227ca6cc")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("Apt", "Debian Advanced Package Tool.", "74528ec1-d223-40ad-8e2f-703b7f6d33b0")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("Yum", "YellowDog Updater, Modified.", "27a03a73-d654-4999-8888-72043c84172d")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("Zypp", "ZYpp Package Manager.", "8a016a93-73f3-4b3d-9129-b3708f5d1afe")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        createNode(new TypeNode("OptZip", "The very simple unzip-in /opt package manager.",
                "2115cfc1-a2e8-4efe-aa58-60ef64a729ca")
                .setIsa(typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc")));
        return true;
    }
}
