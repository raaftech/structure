package com.raaftech.structure.graph.schema.templates;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TemplateNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

public class TemplatesR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("TemplateModel", "Templates provided by Structure.",
                "e2099a13-9d81-4d78-8b35-2858f822a730", 1)
                .requires("ServerModel", 1)
                .requires("ClientModel", 1)
                .requires("CloudSystemModel", 1)
                .requires("EmulatorModel", 1)
                .requires("HypervisorModel", 1)
                .requires("OperatingSystemModel", 1)
                .requires("PackageManagerModel", 1)
                .requires("ServiceManagerModel", 1);

    }

    @Override
    protected void initNodes() {
        // The computer template

        // Generic Template
        TemplateNode operatingSystem = new TemplateNode("OperatingSystem", "Generic operating system.",
                    "1b8e0d72-c23d-4091-a4e6-74d07f1ee6fa")
                .setIsa("f05e325e-1aee-4892-96e4-8bcd6e97c528")
                .setUnique(true)
                .setRequired(true)
                .setStartOfTemplate(true);

        TemplateNode client = new TemplateNode("Client", "Generic client software.",
                    "e8e3c643-a520-4d63-9088-dd27086a1879")
                .setIsa("8f371b1b-52a3-4409-be70-9cd947bec8dd");

        TemplateNode server = new TemplateNode("Server", "Generic server software.",
                    "54126ca8-3a5e-43b9-a501-98c8d158049d")
                .setIsa("3fb4603b-281e-47c0-b338-768ea05d4660");

        TemplateNode interpreter = new TemplateNode("Interpreter", "Generic interpreter.",
                    "2741cc5a-eb45-4532-9a64-c715c07f93bc")
                .setIsa("d047f415-734c-4bd3-92b9-d68d5521ce69")
                .setRequired(true);

        TemplateNode ipv4Address = new TemplateNode("Ipv4Address", "An IPv4 address.",
                    "0e843d97-c975-42ca-813c-c91f42a7d3cc")
                .setIsa("f7fbe917-9d49-4df5-95f6-6e132067a424")
                .setRequired(true);

        TemplateNode dnsName = new TemplateNode("DnsName", "The short or fully qualified domain name.",
                    "94d10031-5246-4233-b25d-b5f8ef9d0b41")
                .setIsa("99c0da37-3599-418d-8ecc-de36d37ae9f4")
                .setRequired(true)
                .setUnique(true);

        TemplateNode serviceManager = new TemplateNode("ServiceManager", "Generic service manager.",
                    "34bfe99f-1d62-48cb-a8a3-e913b69ef225")
                .setIsa("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3")
                .setRequired(false)
                .setUnique(true);

        operatingSystem
                .addComponent(client)
                .addComponent(server)
                .addComponent(interpreter)
                .addComponent(ipv4Address)
                .addComponent(dnsName)
                .addComponent(serviceManager);

        operatingSystem.save();
    }
}
