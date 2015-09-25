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

        // The computer template

        // Hardware
        TemplateNode componentalGroup = new TemplateNode("ee92c308-80f4-40b2-af3a-77d26cd99517")
                .setUnique(true)
                .setRequired(true)
                .setStartOfTemplate(true);

        TemplateNode computer = new TemplateNode("ec11db91-6973-4285-b018-b461fbb432d7")
                .setUnique(true)
                .setRequired(true);

        TemplateNode processingUnit = new TemplateNode("b7b2a2ff-3af7-469b-afbf-e52f1b80be91");
        TemplateNode volatileMemory = new TemplateNode("44131d46-c1b0-4030-ae5a-9a3108627749");
        TemplateNode nonVolatileMemory = new TemplateNode("7f129e53-ff07-4ed8-9062-daf5f733a3a6");
        TemplateNode consoleDevice = new TemplateNode("3d0865a2-3ecf-4035-886b-0bb603c0e2c2");
        TemplateNode graphicsDevice = new TemplateNode("a642b995-c073-4c27-a611-c5bf3854958f");
        TemplateNode networkDevice = new TemplateNode("86786ed6-d62a-4011-9ae8-0082000e48fa");

        componentalGroup
                .addComponent(computer);
        computer
                .addComponent(processingUnit)
                .addComponent(volatileMemory)
                .addComponent(nonVolatileMemory)
                .addComponent(consoleDevice)
                .addComponent(graphicsDevice)
                .addComponent(networkDevice);

        // Software

        TemplateNode operatingSystem = new TemplateNode("f05e325e-1aee-4892-96e4-8bcd6e97c528")
                .setUnique(true)
                .setRequired(true);

        TemplateNode serviceManager = new TemplateNode("c3008eec-b454-4ef6-bf7e-0bd92c13d3c3");
        TemplateNode packageManager = new TemplateNode("e2b8b9ee-b66f-4252-8fba-4fb172f741bc");
        TemplateNode interpreter = new TemplateNode("d047f415-734c-4bd3-92b9-d68d5521ce69");
        TemplateNode hypervisor = new TemplateNode("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81");
        TemplateNode emulator = new TemplateNode("59aeae81-9741-4986-b9c2-931504cfc9e3");
        // TODO: Change to server when STR-35 is merged
        TemplateNode service = new TemplateNode("3fb4603b-281e-47c0-b338-768ea05d4660");
        // TODO: Add client when STR-35 is merged

        componentalGroup.addComponent(operatingSystem);

        operatingSystem
                .addComponent(serviceManager)
                .addComponent(packageManager)
                .addComponent(interpreter)
                .addComponent(hypervisor)
                .addComponent(emulator)
                .addComponent(service);

        // Configuration
        // TODO: what configures what?
        TemplateNode configuration = new TemplateNode("3f4a43f7-b558-4bf0-ba7e-66b99cffaf16");

        componentalGroup.addComponent(configuration);

        componentalGroup.save();
    }
}
