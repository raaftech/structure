package com.raaftech.structure.graph.schema.emulators;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of EmulatorsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class EmulatorsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("EmulatorModel", "Main emulator types.",
                "ce00fbd8-b9e6-4e93-ae5e-ce0d9da5b3b4", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.emulators-0.0.1
        TypeNode emulator = typeNodeRepository.findByUuid("59aeae81-9741-4986-b9c2-931504cfc9e3");
        addToMap(new TypeNode("Bochs", "The Bochs Emulator.", "cd8f940e-219b-488e-8b75-d9fece8b3b35")
                .setIsa(emulator));
        addToMap(new TypeNode("DosBox", "The DOSBox Emulator.", "7e0135dc-b720-440a-9f4e-76747f7a7fe5")
                .setIsa(emulator));
        addToMap(new TypeNode("Qemu", "The QEMU Emulator.", "c80fddb7-bdbf-44b9-ac54-f7dd7ce4dbd0")
                .setIsa(emulator));
    }
}
