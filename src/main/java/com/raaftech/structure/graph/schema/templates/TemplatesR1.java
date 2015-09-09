package com.raaftech.structure.graph.schema.templates;

import java.util.Arrays;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

public class TemplatesR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("TemplateModel", "Templates provided by Structure.",
                "e2099a13-9d81-4d78-8b35-2858f822a730", 1)
                .requires("CapabilityHandlerModel", 1);

    }

    @Override
    protected void initNodes() {
        typeNodeList = Arrays.asList();

        TemplateNode server, networkInterface, operatingSystem, ipv4Listener;

        server = new TemplateNode("ec11db91-6973-4285-b018-b461fbb432d7")
                .setUnique(true)
                .setRequired(true)
                .setStartOfTemplate(true);

        networkInterface = new TemplateNode("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf")
                .setRequired(true)
                .setIsa("0d4c880d-d888-435f-b3a3-3d3eb9e70ddf");

        operatingSystem = new TemplateNode("f05e325e-1aee-4892-96e4-8bcd6e97c528")
                .setUnique(true)
                .setRequired(true);

        server.addComponent(networkInterface);
        server.addComponent(operatingSystem);

        ipv4Listener = new TemplateNode("5ca95f7a-5175-4e7f-8a61-862e24485af3");

        operatingSystem.addComponent(ipv4Listener);

        server.save();
    }
}
