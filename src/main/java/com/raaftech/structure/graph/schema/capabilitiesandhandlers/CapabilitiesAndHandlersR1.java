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

        /***********************************/
        /* Checkable capabilities/handlers */
        /***********************************/
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

        /*************************************/
        /* Commandable capabilities/handlers */
        /*************************************/
        TypeNode commandHandlerType = typeNodeRepository.findByUuid("6ac7f3f8-4a1f-42aa-87b8-b2f5516b5d7c");

        obj = mapper.createObjectNode()
                .put("name", "SshCommandHandler")
                .put("desc", "Run a command over SSH.")
                .put("plugin", "raaftech_sshexec");
        InstanceNode commandableHandlerInstance = new InstanceNode(obj, commandHandlerType, "e760cea4-91e6-42d3-95f7-16ce2047ee64");
        TypeNode ssh = typeNodeRepository.findByName("SSH");
        InstanceNode commandableCapability = new InstanceNode("Commandable", "commandable", "76fb3116-d4a8-459a-b62b-a2641bcb7e7d");
        commandableCapability.addHandler(commandableHandlerInstance);
        ssh.addCapability(commandableCapability);

        /**************************************/
        /* Discoverable capabilities/handlers */
        /**************************************/
        TypeNode discoverHandlerType = typeNodeRepository.findByUuid("c5e838c7-5b61-47ff-9285-aa5a108f6867");

        obj = mapper.createObjectNode()
                .put("name", "OperatingSystemDiscoveryHandler")
                .put("desc", "Discover the type of OperatingSystem.")
                .put("plugin", "raaftech_discoveros");
        InstanceNode discoverHandlerInstance = new InstanceNode(obj, discoverHandlerType, "a8ffe716-3422-45cc-9783-596802bc4a62");
        TypeNode operatingSystem = typeNodeRepository.findByName("OperatingSystem");
        InstanceNode discoverCapability = new InstanceNode("Discoverable", "Discoverable", "3c170a27-05ee-4999-97d4-e7cc9135cb96");
        discoverCapability.addHandler(discoverHandlerInstance);
        operatingSystem.addCapability(discoverCapability);

        obj = mapper.createObjectNode()
                .put("name", "ComputerDiscoveryHandler")
                .put("desc", "Discover a computer.")
                .put("plugin", "raaftech_discovercomputer");
        discoverHandlerInstance = new InstanceNode(obj, discoverHandlerType, "4ccaee5d-4f58-4027-a043-1e2ba09665f4");
        TypeNode computer = typeNodeRepository.findByName("Computer");
        discoverCapability = new InstanceNode("Discoverable", "Discoverable", "3c170a27-05ee-4999-97d4-e7cc9135cb96");
        discoverCapability.addHandler(discoverHandlerInstance);
        computer.addCapability(discoverCapability);

        obj = mapper.createObjectNode()
                .put("name", "NicDiscoveryHandler")
                .put("desc", "Discover a nic.")
                .put("plugin", "raaftech_discovernic");
        discoverHandlerInstance = new InstanceNode(obj, discoverHandlerType, "db3f4ba6-35e6-4c40-b127-eb90b27fe514");
        TypeNode nic = typeNodeRepository.findByName("WiredNetworkDevice");
        discoverCapability = new InstanceNode("Discoverable", "Discoverable", "3c170a27-05ee-4999-97d4-e7cc9135cb96");
        discoverCapability.addHandler(discoverHandlerInstance);
        nic.addCapability(discoverCapability);

        obj = mapper.createObjectNode()
                .put("name", "Ipv4ListenerDiscoveryHandler")
                .put("desc", "Discover a ipv4 listener.")
                .put("plugin", "raaftech_discoveripv4listeners");
        discoverHandlerInstance = new InstanceNode(obj, discoverHandlerType, "ba28b10b-8792-4bf8-8179-4ed5b62163e6");
        TypeNode ipv4listener = typeNodeRepository.findByName("Ipv4Listener");
        discoverCapability = new InstanceNode("Discoverable", "Discoverable", "3c170a27-05ee-4999-97d4-e7cc9135cb96");
        discoverCapability.addHandler(discoverHandlerInstance);
        ipv4listener.addCapability(discoverCapability);
    }
}
