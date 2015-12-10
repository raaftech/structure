package com.raaftech.structure.graph.schema.templates;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

public class TemplatesR2 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("TemplateModel", "Templates provided by Structure.",
                "ae3d9217-a1af-42a5-a3e1-266ef36815ab", 2)
                .requires("TemplateModel", 1);

    }

    @Override
    protected void initNodes() {
        typeNodeList = Arrays.asList();

        // Linux

        TemplateNode operatingSystem = new TemplateNode("a5613527-2f64-4481-83f2-80b0cb427e28")
                .setUnique(true)
                .setRequired(true)
                .setStartOfTemplate(true);

        TemplateNode systemctl = new TemplateNode("c409c9de-e23d-429b-a59a-ac46ea296331");
        TemplateNode packageManager = new TemplateNode("e2b8b9ee-b66f-4252-8fba-4fb172f741bc");
        TemplateNode interpreter = new TemplateNode("6d467b28-449f-498c-9ca5-8660f522897f");
        TemplateNode ipv4Address = new TemplateNode("f7fbe917-9d49-4df5-95f6-6e132067a424");
        TemplateNode sshServer = new TemplateNode("7db4a3ef-2d3c-4071-929b-e81505fbd5e2");
        TemplateNode server =  new TemplateNode("3fb4603b-281e-47c0-b338-768ea05d4660");
        TemplateNode sshClient = new TemplateNode("9e977808-78fd-4eb6-95e0-ff7c7011caa5");
        TemplateNode winExe = new TemplateNode("2f207919-c852-4177-9b8f-e4a368026ca6");
        TemplateNode iputils = new TemplateNode("6619adef-3bcb-4e32-bfec-d235a7bf3e29");
        // TODO: Change to server when STR-35 is merged
//        TemplateNode service = new TemplateNode("3fb4603b-281e-47c0-b338-768ea05d4660");
        // TODO: Add client when STR-35 is merged

        operatingSystem
                .addComponent(systemctl)
                .addComponent(packageManager)
                .addComponent(interpreter)
                .addComponent(ipv4Address)
                .addComponent(sshServer)
                .addComponent(server)
                .addComponent(sshClient)
                .addComponent(winExe)
                .addComponent(iputils);

        operatingSystem.save();


        // BSD
        operatingSystem = new TemplateNode("6004436f-7c88-4c2e-a29d-e8077bad10e9")
                .setUnique(true)
                .setRequired(true)
                .setStartOfTemplate(true);

        TemplateNode rc = new TemplateNode("67d7aa27-e0d9-458c-89fb-82734a8def95");
        interpreter = new TemplateNode("fd95e858-c788-4578-9223-122eceb2e6c6");
        ipv4Address = new TemplateNode("f7fbe917-9d49-4df5-95f6-6e132067a424");
        sshServer = new TemplateNode("7db4a3ef-2d3c-4071-929b-e81505fbd5e2");
        server =  new TemplateNode("3fb4603b-281e-47c0-b338-768ea05d4660");

        operatingSystem
                .addComponent(rc)
                .addComponent(interpreter)
                .addComponent(ipv4Address)
                .addComponent(sshServer)
                .addComponent(server);

        operatingSystem.save();

        // Windows
        operatingSystem = new TemplateNode("060a519d-08d0-4d12-82ac-1e3f14277009")
                .setStartOfTemplate(true)
                .setRequired(true)
                .setUnique(true);

        TemplateNode services = new TemplateNode("21b82fbf-803c-4056-b914-502e2ff9952d");
        interpreter = new TemplateNode("bc101145-dde2-4363-a7ed-7091d2ff2621");
        ipv4Address = new TemplateNode("f7fbe917-9d49-4df5-95f6-6e132067a424");
        TemplateNode smbServer = new TemplateNode("65af133b-1a2c-4faa-8d2a-a288eaa4f412");
        server =  new TemplateNode("3fb4603b-281e-47c0-b338-768ea05d4660");

        operatingSystem
                .addComponent(services)
                .addComponent(interpreter)
                .addComponent(ipv4Address)
                .addComponent(smbServer)
                .addComponent(server);

        operatingSystem.save();
    }
}
