package com.raaftech.structure.graph.schema.clients;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Created by boy on 9/9/15.
 */
public class ClientsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation()

    {
        updateInformation = SchemaNode.getOrCreateSchemaNode("ClientModel", "Main client types.",
                "3d04bcbd-f20a-499a-827a-c0495b558911", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        TypeNode client = typeNodeRepository.findByUuid("8f371b1b-52a3-4409-be70-9cd947bec8dd");
        addToMap(new TypeNode("HttpClient", "A generic HTTP Client.", "fec4ca89-8e2d-4562-a25c-0638eef3a525")
                .setIsa(client));

        // Remote Access Clients
        addToMap(new TypeNode("RemoteAccessClient", "A generic remote access client.", "bf1826d4-cff3-4b76-8cc9-cc752560a4bb")
                .setIsa(client));
        addToMap(new TypeNode("SmbClient", "A generic SMB/CIFS Client.", "7d1c7242-2403-4578-b6c4-789573ea3c45")
                .setIsa(getFromMap("bf1826d4-cff3-4b76-8cc9-cc752560a4bb")));

        // Telnet clients
        addToMap(new TypeNode("TelnetClient", "A generic TELNET Client.", "442f0c59-c264-4227-9b50-2ef2b90af968")
                .setIsa(getFromMap("bf1826d4-cff3-4b76-8cc9-cc752560a4bb")));
        addToMap(new TypeNode("GnuTelnetClient", "GNU telnet client from GNU inetutils.", "662490c0-778a-46e5-bc9a-c0c6cec17422")
                .setIsa(getFromMap("bf1826d4-cff3-4b76-8cc9-cc752560a4bb")));

        // SSH clients
        addToMap(new TypeNode("SshClient", "A generic SSH Client.", "dce98afb-1a42-48af-8b4b-ab9d08b3b5dc")
                .setIsa(getFromMap("bf1826d4-cff3-4b76-8cc9-cc752560a4bb")));
        addToMap(new TypeNode("OpenSshClient", "The OpenSSH client.", "9e977808-78fd-4eb6-95e0-ff7c7011caa5")
                .setIsa(getFromMap("dce98afb-1a42-48af-8b4b-ab9d08b3b5dc")));

        // Smb clients
        addToMap(new TypeNode("Winexe", "A *nix smb client.", "2f207919-c852-4177-9b8f-e4a368026ca6")
                .setIsa(getFromMap("7d1c7242-2403-4578-b6c4-789573ea3c45")));
        addToMap(new TypeNode("Samba", "A *nix smb toolchain.", "f513ddfc-e50b-49b4-9bd8-64afc61ff7a5")
                .setIsa(getFromMap("7d1c7242-2403-4578-b6c4-789573ea3c45")));

        // ICMP clients
        addToMap(new TypeNode("Iputils", "Network monitoring tools, including ping.", "6619adef-3bcb-4e32-bfec-d235a7bf3e29")
                .setIsa(client));
    }
}
