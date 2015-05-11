package com.raaftech.structure.graph.schema.capabilitiesandhandlers;

import java.util.Arrays;

import com.raaftech.structure.graph.InstanceNode;
import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of CapabilitiesAndHandlersR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class CapabilitiesAndHandlersR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("CapabilityHandlerModel", "Implement Capabilities & Handlers.",
                "538e148b-6138-4d3f-adee-6cddff8e1631", 1)
                .requires("ServiceModel", 1)
                .requires("CloudSystemModel", 1)
                .requires("EmulatorModel", 1)
                .requires("HypervisorModel", 1)
                .requires("OperatingSystemModel", 1)
                .requires("PackageManagerModel", 1)
                .requires("ServiceManagerModel", 1);
    }

    @Override
    protected void initNodes() {
        typeNodeList = Arrays.asList();

        TypeNode statusHandlerType = typeNodeRepository.findByUuid("3b6555cd-abd4-4cc6-ab64-b7c031c2b008");
        InstanceNode statusHandlerInstance = new InstanceNode("IcmpStatusHandler",
                "Check status with ICMP Echo Request.", statusHandlerType, "d1a528e0-728d-412e-8115-ccaa5c1dfd4d");
        TypeNode ipv4Address = typeNodeRepository.findByName("Ipv4Address");
        InstanceNode checkableCapability = new InstanceNode("Checkable", "checkable", "b3cfd296-1090-43c0-ac2e-00e5d7fe7779");
        checkableCapability.addHandler(statusHandlerInstance);
        ipv4Address.addCapability(checkableCapability);
    }
}
