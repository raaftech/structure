package com.raaftech.structure.graph.schema.servers;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of ServersR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class ServersR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("ServerModel", "Main server types.",
                "f947e94a-f9b2-4170-90e1-107c2a946bd7", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.servers-0.0.1
        TypeNode server = typeNodeRepository.findByUuid("3fb4603b-281e-47c0-b338-768ea05d4660");
        addToMap(new TypeNode("HttpServer", "A generic HTTP Server.", "b46cb520-caaf-4d26-b59d-58edca723b8d", 0x0066)
                .setIsa(server));
        addToMap(new TypeNode("SmbServer", "A generic SMB/CIFS Server.", "65af133b-1a2c-4faa-8d2a-a288eaa4f412", 0x00D4)
                .setIsa(server));
        addToMap(new TypeNode("SshServer", "A generic SSH Server.", "7db4a3ef-2d3c-4071-929b-e81505fbd5e2", 0x00DE)
                .setIsa(server));
        addToMap(new TypeNode("TelnetServer", "A generic TELNET Server.", "4a09bcce-59c6-4e94-9fc4-ba020733a27c", 0x00E8)
                .setIsa(server));
    }
}
