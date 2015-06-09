package com.raaftech.structure.graph.schema.capabilitiesandhandlers;

import java.util.Arrays;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        ObjectMapper mapper = new ObjectMapper();

        TypeNode statusHandlerType = typeNodeRepository.findByUuid("3b6555cd-abd4-4cc6-ab64-b7c031c2b008");
        ObjectNode obj = mapper.createObjectNode()
                .put("name", "IcmpStatusHandler")
                .put("desc", "Check status with ICMP Echo Request.")
                .put("plugin", "raaftech_pingcheck");
        InstanceNode statusHandlerInstance = new InstanceNode(obj, statusHandlerType, "d1a528e0-728d-412e-8115-ccaa5c1dfd4d");
        TypeNode ipv4Address = typeNodeRepository.findByName("Ipv4Address");
        InstanceNode checkableCapability = new InstanceNode("Checkable", "checkable", "b3cfd296-1090-43c0-ac2e-00e5d7fe7779");
        checkableCapability.addHandler(statusHandlerInstance);
        ipv4Address.addCapability(checkableCapability);

        obj = mapper.createObjectNode()
                .put("name", "ConnectionStatusHandler")
                .put("desc", "Check status by connecting to a port.")
                .put("plugin", "raaftech_portcheck");
        statusHandlerInstance = new InstanceNode(obj, statusHandlerType, "2ef93146-c33c-4343-93c9-ac4cd1baf818");
        TypeNode ipv4Listener = typeNodeRepository.findByName("Ipv4Listener");
        checkableCapability = new InstanceNode("Checkable", "checkable", "b3cfd296-1090-43c0-ac2e-00e5d7fe7779");
        checkableCapability.addHandler(statusHandlerInstance);
        ipv4Listener.addCapability(checkableCapability);

        obj = mapper.createObjectNode()
                .put("name", "SshCommandHandler")
                .put("desc", "Run a command over SSH.")
                .put("plugin", "raaftech_sshexec");
        InstanceNode commandableHandlerInstance = new InstanceNode(obj, statusHandlerType, "e760cea4-91e6-42d3-95f7-16ce2047ee64");
        TypeNode ssh = typeNodeRepository.findByName("SSH");
        InstanceNode commandableCapability = new InstanceNode("Commandable", "commandable", "76fb3116-d4a8-459a-b62b-a2641bcb7e7d");
        commandableCapability.addHandler(commandableHandlerInstance);
        ssh.addCapability(commandableCapability);
    }
}
