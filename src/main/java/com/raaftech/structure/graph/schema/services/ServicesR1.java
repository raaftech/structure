package com.raaftech.structure.graph.schema.services;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

/**
 * Give a description of ServicesR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class ServicesR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("ServiceModel", "Main service types.",
                "f947e94a-f9b2-4170-90e1-107c2a946bd7", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.services-0.0.1
        typeNodeList = Arrays.asList(
            new TypeNode("HttpServer", "A generic HTTP Server.", "b46cb520-caaf-4d26-b59d-58edca723b8d")
                    .setIsa("3fb4603b-281e-47c0-b338-768ea05d4660"),
            new TypeNode("SmbServer", "A generic SMB/CIFS Server.", "65af133b-1a2c-4faa-8d2a-a288eaa4f412")
                    .setIsa("3fb4603b-281e-47c0-b338-768ea05d4660"),
            new TypeNode("SshServer", "A generic SSH Server.", "7db4a3ef-2d3c-4071-929b-e81505fbd5e2")
                    .setIsa("3fb4603b-281e-47c0-b338-768ea05d4660"),
            new TypeNode("TelnetServer", "A generic TELNET Server.", "4a09bcce-59c6-4e94-9fc4-ba020733a27c")
                    .setIsa("3fb4603b-281e-47c0-b338-768ea05d4660")
        );
    }
}
