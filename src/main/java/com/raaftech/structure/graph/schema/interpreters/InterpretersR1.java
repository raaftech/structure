package com.raaftech.structure.graph.schema.interpreters;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

/**
 * Give a description of InterpretersR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class InterpretersR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("InterpreterModel", "Main interpreter types.",
                "452269e5-922e-49af-8442-ef3b58348f61", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.interpreters-0.0.1
        typeNodeList = Arrays.asList(
            new TypeNode("UnixShell", "A generic UNIX shell.", "86168e4a-8c99-43c9-ad95-c8f88d96ac32")
                    .setIsa("d047f415-734c-4bd3-92b9-d68d5521ce69"),
            new TypeNode("Bash", "The Bourne Again Shell.", "6d467b28-449f-498c-9ca5-8660f522897f")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("Ksh", "The Korn Shell.", "fd95e858-c788-4578-9223-122eceb2e6c6")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("Csh", "The C Shell.", "608c9a23-a194-49b0-a986-c34bd52bf691")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("Tcsh", "The TENEX C Shell.", "bb3e3458-391d-4c75-8857-4a08be7e5905")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("Sh", "The Bourne Shell.", "7c1c3278-6eb4-4be5-8b8d-94204bfd2223")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("Zsh", "The Zhong Shao Shell.", "7d9afc14-8e12-4c56-8ca6-4c43fd9808ab")
                    .setIsa("86168e4a-8c99-43c9-ad95-c8f88d96ac32"),
            new TypeNode("DosShell", "A generic DOS shell.", "bc101145-dde2-4363-a7ed-7091d2ff2621")
                    .setIsa("d047f415-734c-4bd3-92b9-d68d5521ce69")
        );
    }
}
