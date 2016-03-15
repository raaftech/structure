package com.raaftech.structure.graph.schema.softwareandprotocols;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * SoftwareAndProtocols couples specific pieces of software
 * to the protocols they implement.
 */
public class SoftwareAndProtocolsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("SoftwareProtocolModel", "Couples software to protocols.",
                "55c64850-87b6-4c43-b434-1129de958ef0", 1)
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
        // Couple OpenSSH client and generic SSH server to SSH 1.0 and 2.0 protocols.
        TypeNode openSshClient = typeNodeRepository.findOneByName("OpenSshClient");
        TypeNode sshServer = typeNodeRepository.findOneByName("SshServer");
        TypeNode ssh10 = typeNodeRepository.findOneByName("SSH10");
        TypeNode ssh20 = typeNodeRepository.findOneByName("SSH20");

        openSshClient.addImplementedProtocol(ssh10);
        openSshClient.addImplementedProtocol(ssh20);
        openSshClient.save();

        sshServer.addImplementedProtocol(ssh10);
        sshServer.addImplementedProtocol(ssh20);
        sshServer.save();

        // Couple GNU Telnet client and generic Telnet server to the Telnet protocol.
        TypeNode gnuTelnetClient = typeNodeRepository.findOneByName("GnuTelnetClient");
        TypeNode telnetServer = typeNodeRepository.findOneByName("TelnetServer");
        TypeNode telnet = typeNodeRepository.findOneByName("TELNET");

        gnuTelnetClient.addImplementedProtocol(telnet);
        gnuTelnetClient.save();

        telnetServer.addImplementedProtocol(telnet);
        telnetServer.save();

        // Couple Smb clients and servers
        TypeNode smbServer = typeNodeRepository.findOneByName("SmbServer");
        TypeNode winExe = typeNodeRepository.findOneByName("Winexe");
        TypeNode samba = typeNodeRepository.findOneByName("Samba");
        TypeNode smb = typeNodeRepository.findOneByName("SMB");

        smbServer.addImplementedProtocol(smb);
        smbServer.save();

        winExe.addImplementedProtocol(smb);
        winExe.save();

        samba.addImplementedProtocol(smb);
        samba.save();
    }

}
