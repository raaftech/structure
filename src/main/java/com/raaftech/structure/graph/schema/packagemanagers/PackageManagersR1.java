package com.raaftech.structure.graph.schema.packagemanagers;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

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
    protected void initNodes() {
        // raaftech.com.model.packagemanagers-0.0.1
        TypeNode packageManager = typeNodeRepository.findByUuid("e2b8b9ee-b66f-4252-8fba-4fb172f741bc");
        addToMap(new TypeNode("PacMan", "Arch Linux Pacman Package Manager.", "ad047d85-792a-4385-a14c-b85266bc5176")
                .setIsa(packageManager));
        addToMap(new TypeNode("BsdPkg", "BSD pkg Package Manager.", "95d6093b-448b-4a62-8642-dc8d32b43622")
                .setIsa(packageManager));
        addToMap(new TypeNode("BsdPkgNg", "BSD pkgng Package Manager.", "1a879371-f680-4514-8189-5d6e227ca6cc")
                .setIsa(packageManager));
        addToMap(new TypeNode("Apt", "Debian Advanced Package Tool.", "74528ec1-d223-40ad-8e2f-703b7f6d33b0")
                .setIsa(packageManager));
        addToMap(new TypeNode("Yum", "YellowDog Updater, Modified.", "27a03a73-d654-4999-8888-72043c84172d")
                .setIsa(packageManager));
        addToMap(new TypeNode("Zypp", "ZYpp Package Manager.", "8a016a93-73f3-4b3d-9129-b3708f5d1afe")
                .setIsa(packageManager));
        addToMap(new TypeNode("OptZip", "The very simple unzip-in /opt package manager.",
                "2115cfc1-a2e8-4efe-aa58-60ef64a729ca")
                .setIsa(packageManager));

        addToMap(new TypeNode("SdUx", "SD/UX, the HP/UX Software Distributor.", "92e1ff01-8f01-4882-be63-ed9d0777924d")
                .setIsa(packageManager));
        addToMap(new TypeNode("AixLpp", "IBM Licensed Program Product Manager.", "5da69c0f-ac1f-4fbf-8365-77c2706874aa")
                .setIsa(packageManager));
        addToMap(new TypeNode("SolarisIps", "Solaris Image Packaging System Manager.", "e70e7353-4e74-4a58-a7ca-eef0005ad5ec")
                .setIsa(packageManager));
        addToMap(new TypeNode("SolarisPkg", "Older Solaris Package System Manager.", "7ed426f2-c163-4bdc-b1a0-0dc05e31f691")
                .setIsa(packageManager));
    }
}
