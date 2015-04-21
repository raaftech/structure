package com.raaftech.structure.graph.schema.hypervisors;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

/**
 * Give a description of HypervisorsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class HypervisorsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("HypervisorModel", "Main hypervisor types.",
                "76fc46bb-8ff8-4e35-a8b5-80c44a82346a", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.hypervisors-0.0.1
        typeNodeList = Arrays.asList(
            new TypeNode("Kvm", "The Linux kernel-based virtual machine.", "04b4bd73-fcec-42f6-bb03-7686ad019fbb")
                    .setIsa("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81"),
            new TypeNode("VirtualBox", "The Oracle VM VirtualBox virtual machine.",
                    "3c47f326-d613-4f42-b4ac-804a6c22f076")
                    .setIsa("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81"),
            new TypeNode("VmwareEsxi", "The VMware ESXi virtual machine.", "b9739ee8-34a6-4308-b7e2-ba775c0e59cf")
                    .setIsa("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81"),
            new TypeNode("VmwareWorkstation", "The VMware Workstation virtual machine.",
                    "897281c1-05be-43aa-a49c-50fd9ed4fc88")
                    .setIsa("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81"),
            new TypeNode("Xen", "The Xen Project virtual machine.", "54916142-640a-41df-82ad-974e92d3371c")
                    .setIsa("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81")
        );
    }
}
