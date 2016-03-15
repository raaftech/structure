package com.raaftech.structure.graph.schema.hypervisors;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

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
        TypeNode hypervisor = typeNodeRepository.findByUuid("c5c7fcb5-8ac3-40cc-b195-b2b6af368e81");
        addToMap(new TypeNode("Kvm", "The Linux kernel-based virtual machine.", "04b4bd73-fcec-42f6-bb03-7686ad019fbb")
                .setIsa(hypervisor));
        addToMap(new TypeNode("VirtualBox", "The Oracle VM VirtualBox virtual machine.",
                "3c47f326-d613-4f42-b4ac-804a6c22f076")
                .setIsa(hypervisor));
        addToMap(new TypeNode("VmwareEsxi", "The VMware ESXi virtual machine.", "b9739ee8-34a6-4308-b7e2-ba775c0e59cf")
                .setIsa(hypervisor));
        addToMap(new TypeNode("VmwareWorkstation", "The VMware Workstation virtual machine.",
                "897281c1-05be-43aa-a49c-50fd9ed4fc88")
                .setIsa(hypervisor));
        addToMap(new TypeNode("Xen", "The Xen Project virtual machine.", "54916142-640a-41df-82ad-974e92d3371c")
                .setIsa(hypervisor));
        addToMap(new TypeNode("HpVm", "The HP Integrity virtual machine.", "af0ecba0-9c1f-43ec-b555-9a665f904dd3")
                .setIsa(hypervisor));
        addToMap(new TypeNode("PowerVm", "The IBM PowerVM virtual machine.", "50a2e964-ed70-4a96-988d-62aa9bfec5a5")
                .setIsa(hypervisor));
        addToMap(new TypeNode("OracleVm", "The Oracle VM Server for SPARC virtual machine.", "b1722662-e3ca-423a-9217-d226e32ab6f4")
                .setIsa(hypervisor));
        addToMap(new TypeNode("VmwareVsphere", "The vSphere virtual machine manager.", "ea00099d-5284-4a34-940f-7c0844e137b1")
                .setIsa(hypervisor));
    }
}
