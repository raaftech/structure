package com.raaftech.structure.graph.schema.cloudsystems;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

import java.util.Arrays;

/**
 * Give a description of CloudSystemsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class CloudSystemsR1 extends SchemaUpdate {

    @Override
    protected void setUpdateInformation() {
        updateInformation = SchemaNode.getOrCreateSchemaNode("CloudSystemModel", "Main cloud (operating) system types.",
                "4a8e288a-696d-4616-bb77-970dcf3e150d", 1)
                .requires("BaseModel", 1);
    }

    @Override
    protected void initNodes() {
        // raaftech.com.model.cloudsystems-0.0.1
        typeNodeList = Arrays.asList(
                new TypeNode("ApacheCloudStack", "The Apache CloudStack platform.",
                        "c1d0eada-0d22-4026-aa2d-d865c1583482")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("VmwareCloudFoundry", "The VMware CloudFoundry platform.",
                        "6d080eef-d9c6-4be7-b81b-b9c57f0fe515")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("Eucalyptus", "The Eucalyptus Cloud platform.", "87c66de2-e24b-4586-8170-5fcf6cc28424")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("OpenNebula", "The OpenNebula Cloud platform.", "0e5b0e12-5975-43f5-9e92-5298a75a5f7a")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("OpenShift", "The OpenShift Cloud platform.", "e57b8fbb-05a0-4c64-830a-8da17801e757")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("OpenStack", "The OpenStack Cloud platform.", "55e9f170-9a98-42e3-ba58-f35c592eaeb0")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("AmazonWebServices", "The Amazon Web Services Cloud platform.", "4cd84c58-3cb3-41c1-89d8-6c2fa70ddb54")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("IbmBlueMix", "The IBM BlueMix Cloud platform.", "7e60d3b9-5837-41c3-bd68-12c850171441")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("CitrixCloudCom", "The Citrix Cloud.com platform.", "a5fafd65-2ffc-4509-8289-5760cd15a293")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("GoogleCloud", "The Google Cloud platform.", "0f48c798-6621-4659-9f13-a37e54ef4568")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("HerokuCloud", "The Heroku Cloud platform.", "26a6929f-ef61-4714-baa0-e72bdbf5960a")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("HpCloud", "The HP Cloud platform.", "e7806e74-9002-4f49-9e87-9adf25490efb")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("JoyentCloud", "The Joyent Cloud platform.", "c60297ae-9fc8-4567-a272-7e2b439b4c57")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("MicrosoftAzure", "The Microsoft Azure Cloud platform.", "171c2c6a-9101-437b-b464-e99d276ded47")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("OnAppCloud", "The OnApp Cloud platform.", "3f368aba-b0f2-4e14-9d7b-715312a7e385")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4"),
                new TypeNode("OracleCloud", "The Oracle Cloud platform.", "fc070f26-87a0-45e8-8ae0-e894fcdb83ea")
                        .setIsa("86c0439a-4219-46e6-82a3-6fb2ee774aa4")
        );
    }
}
