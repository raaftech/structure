package com.raaftech.structure.graph.schema.operatingsystems;

import com.raaftech.structure.graph.SchemaNode;
import com.raaftech.structure.graph.TypeNode;
import com.raaftech.structure.graph.schema.SchemaUpdate;

/**
 * Give a description of OperatingSystemsR1 here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-12-15
 */
public class OperatingSystemsR1 extends SchemaUpdate {

        @Override
        protected void setUpdateInformation() {
                updateInformation = SchemaNode.getOrCreateSchemaNode("OperatingSystemModel", "Main operating system types.",
                        "90d2c69e-7199-45f4-bafa-5e0f848cf17d", 1)
                        .requires("BaseModel", 1);
        }

        @Override
        protected void initNodes() {
                // raaftech.com.model.operatingsystems-0.0.1
                TypeNode operatingSystem = typeNodeRepository.findByUuid("f05e325e-1aee-4892-96e4-8bcd6e97c528");
                addToMap(new TypeNode("DosLike", "A DOS-like operating system.", "e181e9e8-7f30-42a3-b87f-b8e183ddfd6f")
                        .setIsa(operatingSystem));
                addToMap(new TypeNode("WindowsLike", "A Windows-like operating system.", "4edbaf6a-da58-4644-abac-ae80328ddb49")
                        .setIsa(operatingSystem));
                addToMap(new TypeNode("Windows", "The Microsoft Windows Operating System.", "060a519d-08d0-4d12-82ac-1e3f14277009")
                        .setIsa(getFromMap("4edbaf6a-da58-4644-abac-ae80328ddb49")));
                addToMap(new TypeNode("UnixLike", "A UNIX-like operating system.", "6a1eea8b-1462-4439-8c60-12edbee6c397")
                        .setIsa(operatingSystem));
                addToMap(new TypeNode("Unix", "The UNIX Operating System.", "a3b228b6-f737-460c-a3bc-bc653167b36c")
                        .setIsa(getFromMap("6a1eea8b-1462-4439-8c60-12edbee6c397")));
                addToMap(new TypeNode("Illumos", "The Illumos Operating System.", "f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")
                        .setIsa(getFromMap("a3b228b6-f737-460c-a3bc-bc653167b36c")));
                addToMap(new TypeNode("MacOsX", "The Apple Mac OS X Operating System.", "0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")
                        .setIsa(getFromMap("a3b228b6-f737-460c-a3bc-bc653167b36c")));
                addToMap(new TypeNode("Bsd", "The Berkeley Software Distribution Operating System.",
                        "6004436f-7c88-4c2e-a29d-e8077bad10e9")
                        .setIsa(getFromMap("6a1eea8b-1462-4439-8c60-12edbee6c397")));
                addToMap(new TypeNode("Linux", "The Linux Operating System.", "a5613527-2f64-4481-83f2-80b0cb427e28")
                        .setIsa(getFromMap("6a1eea8b-1462-4439-8c60-12edbee6c397")));

                addToMap(new TypeNode("FreeDos", "A free DOS-like operating system.", "0481d915-9d3c-4dbc-a55d-a688652934ef")
                        .setIsa(getFromMap("e181e9e8-7f30-42a3-b87f-b8e183ddfd6f")));
                addToMap(new TypeNode("FreeDos1", "A free DOS-like operating system, version 1.x.", "fd4984a4-e593-491d-ad1a-986fc203ff0e")
                        .setIsa(getFromMap("0481d915-9d3c-4dbc-a55d-a688652934ef")));
                addToMap(new TypeNode("MsDos", "The Microsoft Disk Operating System.", "dfbe4ce9-75be-4431-9629-3ab7d4a311e5")
                        .setIsa(getFromMap("e181e9e8-7f30-42a3-b87f-b8e183ddfd6f")));
                addToMap(new TypeNode("MsDos5", "The Microsoft Disk Operating System, version 5.x.", "50dcae6d-48b0-432f-8f26-1cbf302bf504")
                        .setIsa(getFromMap("dfbe4ce9-75be-4431-9629-3ab7d4a311e5")));
                addToMap(new TypeNode("MsDos6", "The Microsoft Disk Operating System, version 6.x.", "7d9ce51f-5ff4-40a7-b089-770aa0c29f71")
                        .setIsa(getFromMap("dfbe4ce9-75be-4431-9629-3ab7d4a311e5")));
                addToMap(new TypeNode("Windows9x", "The Microsoft Windows 95/98/ME based operating systems.", "f936f4df-d1a6-4596-808a-6e2891a0688e")
                        .setIsa(getFromMap("060a519d-08d0-4d12-82ac-1e3f14277009")));
                addToMap(new TypeNode("Windows95", "The Microsoft Windows 95 operating system.", "37730c0d-e581-4089-8fc0-c848094223a7")
                        .setIsa(getFromMap("f936f4df-d1a6-4596-808a-6e2891a0688e")));
                addToMap(new TypeNode("Windows98", "The Microsoft Windows 98 operating system.", "7d7616b2-2fae-4f47-83cf-0e7add8950c5")
                        .setIsa(getFromMap("f936f4df-d1a6-4596-808a-6e2891a0688e")));
                addToMap(new TypeNode("WindowsMe", "The Microsoft Windows ME operating system.", "231a2bd9-1b36-42e9-97aa-9144a05d9ade")
                        .setIsa(getFromMap("f936f4df-d1a6-4596-808a-6e2891a0688e")));
                addToMap(new TypeNode("WindowsNt", "The Microsoft Windows NT based operating systems.", "3d97107a-fb41-4a45-bd98-e93190ea4e83")
                        .setIsa(getFromMap("060a519d-08d0-4d12-82ac-1e3f14277009")));
                addToMap(new TypeNode("WindowsNt4", "The Microsoft Windows NT 4.0 operating system.", "da560e60-4958-4dee-ab8a-d62d9cdf3593")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows2000", "The Microsoft Windows 2000 operating system.", "9c97e86c-24b5-44b8-bef2-4ad465bb5c82")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("WindowsXp", "The Microsoft Windows XP operating system.", "31a0496d-9cb4-4aab-b3ed-86ddb566c0f4")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows2003", "The Microsoft Windows Server 2003 operating system.", "8eb5e304-7d31-4645-89e2-8c39fefc7278")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("WindowsVista", "The Microsoft Windows Vista operating system.", "ec9e7f41-cda5-48df-9c22-8f320fff398b")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows7", "The Microsoft Windows 7 operating system.", "a3b0f390-7257-43cb-9b9d-1cef8a4092bf")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows2008", "The Microsoft Windows Server 2008 operating system.", "0cb2b9f2-3dfe-4183-8c0d-519a2b16e68a")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows8", "The Microsoft Windows 8 operating system.", "16934de7-609f-4068-a18e-7eb7452b5f01")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("Windows2012", "The Microsoft Windows Server 2012 operating system.", "46758eba-4463-4779-91d9-9095f9d87097")
                        .setIsa(getFromMap("3d97107a-fb41-4a45-bd98-e93190ea4e83")));
                addToMap(new TypeNode("ReactOs", "The ReactOS Operating System.", "3a80082c-173f-44fc-b42a-1fb381d2e3bc")
                        .setIsa(getFromMap("4edbaf6a-da58-4644-abac-ae80328ddb49")));
                addToMap(new TypeNode("ReactOs03", "The ReactOS 0.3.x Operating System.", "0a6a1bd2-1a6b-461b-aa28-cca977b8faf1")
                        .setIsa(getFromMap("3a80082c-173f-44fc-b42a-1fb381d2e3bc")));
                addToMap(new TypeNode("ReactOs04", "The ReactOS 0.4.x Operating System.", "94708506-de30-4827-b9ef-84f6694c4e3f")
                        .setIsa(getFromMap("3a80082c-173f-44fc-b42a-1fb381d2e3bc")));
                addToMap(new TypeNode("Aix", "The IBM AIX Operating System.", "9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b")
                        .setIsa(getFromMap("a3b228b6-f737-460c-a3bc-bc653167b36c")));
                addToMap(new TypeNode("Aix5", "The IBM AIX 5.x Operating System.", "154fd2d1-db50-4318-bfdf-49a62f11bdb6")
                        .setIsa(getFromMap("9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b")));
                addToMap(new TypeNode("Aix6", "The IBM AIX 6.x Operating System.", "843ff854-ee2a-4279-8dca-7caa24fc0727")
                        .setIsa(getFromMap("9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b")));
                addToMap(new TypeNode("Aix7", "The IBM AIX 7.x Operating System.", "a3f1be8b-f222-4c9d-ad77-d2a5365f418d")
                        .setIsa(getFromMap("9cf01e40-6be5-48bc-88c5-e9c3c8caaa4b")));
                addToMap(new TypeNode("HpUx", "The HP/UX Operating System.", "465b42e6-a7c5-4cf8-96c5-54257f054274")
                        .setIsa(getFromMap("a3b228b6-f737-460c-a3bc-bc653167b36c")));
                addToMap(new TypeNode("HpUx10", "The HP/UX 10.x Operating System.", "416e6138-b048-4ee5-9554-6481ea996edf")
                        .setIsa(getFromMap("465b42e6-a7c5-4cf8-96c5-54257f054274")));
                addToMap(new TypeNode("HpUx1020", "The HP/UX 10.20 Operating System.", "ac59c16a-89d2-499e-8341-5161ffb4f176")
                        .setIsa(getFromMap("416e6138-b048-4ee5-9554-6481ea996edf")));
                addToMap(new TypeNode("HpUx11", "The HP/UX 11.x Operating System.", "1d34dabf-b096-4e43-80c4-7318c81bc1fe")
                        .setIsa(getFromMap("465b42e6-a7c5-4cf8-96c5-54257f054274")));
                addToMap(new TypeNode("HpUx1123", "The HP/UX 11.23 Operating System.", "d5312463-4313-4aea-9ac0-552201a7302c")
                        .setIsa(getFromMap("1d34dabf-b096-4e43-80c4-7318c81bc1fe")));
                addToMap(new TypeNode("HpUx1131", "The HP/UX 11.31 Operating System.", "591557b5-f13a-4070-a043-782830560664")
                        .setIsa(getFromMap("1d34dabf-b096-4e43-80c4-7318c81bc1fe")));
                addToMap(new TypeNode("Solaris", "The Solaris Operating System.", "4190b02e-a832-4316-87c6-d9e9b694e284")
                        .setIsa(getFromMap("a3b228b6-f737-460c-a3bc-bc653167b36c")));
                addToMap(new TypeNode("Solaris8", "The Solaris 8 Operating System.", "8e4b0333-3d59-455f-8de9-1bdd8af5ba96")
                        .setIsa(getFromMap("4190b02e-a832-4316-87c6-d9e9b694e284")));
                addToMap(new TypeNode("Solaris9", "The Solaris 9 Operating System.", "d5156f08-91e5-4608-8c97-cd76434b7636")
                        .setIsa(getFromMap("4190b02e-a832-4316-87c6-d9e9b694e284")));
                addToMap(new TypeNode("Solaris10", "The Solaris 10 Operating System.", "e10cd458-1348-488b-8cc4-01af672828ae")
                        .setIsa(getFromMap("4190b02e-a832-4316-87c6-d9e9b694e284")));
                addToMap(new TypeNode("Solaris11", "The Solaris 11 Operating System.", "3a81f815-a426-4269-bdd9-a7ea7527325c")
                        .setIsa(getFromMap("4190b02e-a832-4316-87c6-d9e9b694e284")));
                addToMap(new TypeNode("OpenIndiana", "The OpenIndiana Operating System.", "f4c41e3a-c59a-42f6-b6c8-b7c9b0df3f0e")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("OpenIndiana151a8", "The OpenIndiana 151a8 Operating System.", "c98dc9c8-bcf3-4a88-a24f-ea4b6a6a0951")
                        .setIsa(getFromMap("f4c41e3a-c59a-42f6-b6c8-b7c9b0df3f0e")));
                addToMap(new TypeNode("NexentaStor", "The NexentaStor Operating System.", "0d73cb36-0f99-417b-a150-cc193ba044b8")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("NexentaStor3", "The NexentaStor 3 Operating System.", "6d90bb5c-bcb7-4c21-b7b9-8ba4aeb753b8")
                        .setIsa(getFromMap("0d73cb36-0f99-417b-a150-cc193ba044b8")));
                addToMap(new TypeNode("NexentaStor4", "The NexentaStor 4 Operating System.", "ef584c3a-b8ec-41da-9435-a9d1f8474dab")
                        .setIsa(getFromMap("0d73cb36-0f99-417b-a150-cc193ba044b8")));
                addToMap(new TypeNode("SmartOs", "The SmartOS Operating System.", "a7fba50f-5ab5-46d2-acf7-08fbe7c41b44")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("Dyson", "The Dyson Operating System.", "70e37ec8-e807-4a82-b968-f596dac97d62")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("OmniOs", "The OmniOS Operating System.", "e3e34862-6d23-4922-81cd-fb0df4624cb5")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("OmniOsR151006", "The OmniOS r151006 Operating System.", "8e008afd-d1eb-48bd-8f71-684270750927")
                        .setIsa(getFromMap("e3e34862-6d23-4922-81cd-fb0df4624cb5")));
                addToMap(new TypeNode("OmniOsR151010", "The OmniOS r151010 Operating System.", "e164b0ef-707f-48da-a5b7-b185746c4fd0")
                        .setIsa(getFromMap("e3e34862-6d23-4922-81cd-fb0df4624cb5")));
                addToMap(new TypeNode("OpenSxce", "The OpenSXCE Operating System.", "2bd660a2-1783-4433-81a0-23d82dea1cce")
                        .setIsa(getFromMap("f0bcc651-3ca6-4d4c-ac9d-5a60b2b08520")));
                addToMap(new TypeNode("OpenSxce2013", "The OpenSXCE 2013 Operating System.", "1724894c-bb76-494f-b999-b467b0f4ccb6")
                        .setIsa(getFromMap("2bd660a2-1783-4433-81a0-23d82dea1cce")));
                addToMap(new TypeNode("OpenSxce2014", "The OpenSXCE 2014 Operating System.", "c94f703d-7a1b-4ce4-aac5-c786e59117e0")
                        .setIsa(getFromMap("2bd660a2-1783-4433-81a0-23d82dea1cce")));
                addToMap(new TypeNode("MacOsX3", "The Apple Mac OS X 10.3 Operating System.", "edcd8303-03f6-459c-a5c3-2ea00c20f29c")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX4", "The Apple Mac OS X 10.4 Operating System.", "3d986bda-2109-4e91-a0ec-ad2be3bae792")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX5", "The Apple Mac OS X 10.5 Operating System.", "1cbf8f53-496b-4421-a805-e5bc86c2a56c")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX6", "The Apple Mac OS X 10.6 Operating System.", "eae5479b-1c29-41c9-9b8c-28f6663f85b6")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX7", "The Apple Mac OS X 10.7 Operating System.", "9c9d4b4c-4896-4a67-873f-2638a3ce4616")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX8", "The Apple Mac OS X 10.8 Operating System.", "6d7fc436-1e34-4538-ad6c-e3d8d47aa4b0")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX9", "The Apple Mac OS X 10.9 Operating System.", "8453b53d-4c0c-4892-b7c0-390314978a2b")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("MacOsX10", "The Apple Mac OS X 10.10 Operating System.", "66ef29ff-374d-492f-99fe-e6573fcbacdf")
                        .setIsa(getFromMap("0f81d6dc-d221-464a-ab85-1e3e5d98c3f0")));
                addToMap(new TypeNode("FreeBsd", "The FreeBSD Operating System.", "6bccb300-554a-43ce-981b-fd07b22c83f0")
                        .setIsa(getFromMap("6004436f-7c88-4c2e-a29d-e8077bad10e9")));
                addToMap(new TypeNode("FreeBsd4", "The FreeBSD 4.x Operating System.", "d636ce7f-0da9-434c-b200-e7e409465076")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd5", "The FreeBSD 5.x Operating System.", "931e52b3-97b8-43bb-bf4c-3424ba4cce0f")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd6", "The FreeBSD 6.x Operating System.", "ac25708a-4b29-41c7-9739-c41fbdc38782")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd7", "The FreeBSD 7.x Operating System.", "a05c4070-c1f7-400f-8ff4-00a2ebf613fa")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd8", "The FreeBSD 8.x Operating System.", "4eff136f-1f84-4cde-984d-ecd274cd2610")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd9", "The FreeBSD 9.x Operating System.", "f9a36f71-9b07-4813-ba2c-35448155e6f3")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("FreeBsd10", "The FreeBSD 10.x Operating System.", "e144cfb5-664a-4542-9125-74da72cc0a8a")
                        .setIsa(getFromMap("6bccb300-554a-43ce-981b-fd07b22c83f0")));
                addToMap(new TypeNode("NetBsd", "The NetBSD Operating System.", "33b00a7e-0ddc-4d9e-87d2-876653655d72")
                        .setIsa(getFromMap("6004436f-7c88-4c2e-a29d-e8077bad10e9")));
                addToMap(new TypeNode("NetBsd4", "The NetBSD 4.x Operating System.", "e0dfa561-5500-49dd-a532-f5416385d66a")
                        .setIsa(getFromMap("33b00a7e-0ddc-4d9e-87d2-876653655d72")));
                addToMap(new TypeNode("NetBsd5", "The NetBSD 5.x Operating System.", "64cfc48a-0626-4c72-9634-86dd33e94cde")
                        .setIsa(getFromMap("33b00a7e-0ddc-4d9e-87d2-876653655d72")));
                addToMap(new TypeNode("NetBsd6", "The NetBSD 6.x Operating System.", "8ac7f26d-588d-44de-8792-26d6d429f9ff")
                        .setIsa(getFromMap("33b00a7e-0ddc-4d9e-87d2-876653655d72")));
                addToMap(new TypeNode("OpenBsd", "The OpenBSD Operating System.", "5609a5b8-b1fb-4ec3-a297-155c680a7e65")
                        .setIsa(getFromMap("6004436f-7c88-4c2e-a29d-e8077bad10e9")));
                addToMap(new TypeNode("OpenBsd4", "The OpenBSD 4.x Operating System.", "03b4f697-c07a-473f-aae6-92d30e63c2f3")
                        .setIsa(getFromMap("5609a5b8-b1fb-4ec3-a297-155c680a7e65")));
                addToMap(new TypeNode("OpenBsd5", "The OpenBSD 5.x Operating System.", "6330dd4e-92bc-4cab-b218-1c18d37e9742")
                        .setIsa(getFromMap("5609a5b8-b1fb-4ec3-a297-155c680a7e65")));
                addToMap(new TypeNode("DragonFlyBsd", "The DragonFlyBSD Operating System.", "41d52588-c248-4018-ac34-a8dfc1204729")
                        .setIsa(getFromMap("6004436f-7c88-4c2e-a29d-e8077bad10e9")));
                addToMap(new TypeNode("DragonFlyBsd1", "The DragonFlyBSD 1.x Operating System.", "85d606c8-1d4b-4657-a67c-19ad78c51117")
                        .setIsa(getFromMap("41d52588-c248-4018-ac34-a8dfc1204729")));
                addToMap(new TypeNode("DragonFlyBsd2", "The DragonFlyBSD 2.x Operating System.", "32fd5872-9894-4284-ac52-9b6fdf68f3a8")
                        .setIsa(getFromMap("41d52588-c248-4018-ac34-a8dfc1204729")));
                addToMap(new TypeNode("DragonFlyBsd3", "The DragonFlyBSD 3.x Operating System.", "11666f58-6150-4a65-abea-b8c5fbddfde4")
                        .setIsa(getFromMap("41d52588-c248-4018-ac34-a8dfc1204729")));
                addToMap(new TypeNode("ArchLinux", "The Arch Linux Operating System.", "42a991a2-2f25-479a-a21d-7a7e07f691cf")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Debian", "The Debian Linux Operating System.", "021488da-3011-4861-97a4-420a7d647e5d")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Debian4", "The Debian 4.x Linux Operating System.", "3de571df-b7cf-4673-8a31-e40e5dde624e")
                        .setIsa(getFromMap("021488da-3011-4861-97a4-420a7d647e5d")));
                addToMap(new TypeNode("Debian5", "The Debian 5.x Linux Operating System.", "4f764a05-7fbd-4810-a069-a7e76f4933cd")
                        .setIsa(getFromMap("021488da-3011-4861-97a4-420a7d647e5d")));
                addToMap(new TypeNode("Debian6", "The Debian 6.x Linux Operating System.", "6cf7eed4-847a-4249-b4cd-e0668091336e")
                        .setIsa(getFromMap("021488da-3011-4861-97a4-420a7d647e5d")));
                addToMap(new TypeNode("Debian7", "The Debian 7.x Linux Operating System.", "368ea969-197b-4e18-8a73-c6b6b26beb9f")
                        .setIsa(getFromMap("021488da-3011-4861-97a4-420a7d647e5d")));
                addToMap(new TypeNode("Debian8", "The Debian 8.x Linux Operating System.", "e7cfb97e-b1b0-4696-92bf-a70499d3fb40")
                        .setIsa(getFromMap("021488da-3011-4861-97a4-420a7d647e5d")));
                addToMap(new TypeNode("Ubuntu", "The Ubuntu Linux Operating System.", "1e13acac-4efb-4e62-894d-812492f6c266")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Ubuntu8", "The Ubuntu 8.x Linux Operating System.", "b41dabe4-e012-4c83-b073-4ca202950c44")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu9", "The Ubuntu 9.x Linux Operating System.", "a404e00c-46d7-4c20-940e-e071967dd2fe")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu10", "The Ubuntu 10.x Linux Operating System.", "d98635d0-a229-4755-88e2-30f891ce692d")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu11", "The Ubuntu 11.x Linux Operating System.", "dbb94e1f-72ca-4d2d-a5fd-38d7639df5c1")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu12", "The Ubuntu 12.x Linux Operating System.", "0cf76620-de1f-4e7b-b51b-74ecd980c253")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu13", "The Ubuntu 13.x Linux Operating System.", "ab6a2022-57f1-476c-8013-3cda7554280e")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Ubuntu14", "The Ubuntu 14.x Linux Operating System.", "61a034bb-19e3-4d8f-bda9-35c90e874dc8")
                        .setIsa(getFromMap("1e13acac-4efb-4e62-894d-812492f6c266")));
                addToMap(new TypeNode("Mint", "The Linux Mint Operating System.", "7d5b895a-a076-491d-b6ca-58eed0d0c960")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Mint10", "The Linux Mint 10.x Operating System.", "ab5e364d-51c3-4f77-b1d7-a12128e62f9f")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint11", "The Linux Mint 11.x Operating System.", "27a69b62-9e0b-49e1-bb5e-30cd83e6cc68")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint12", "The Linux Mint 12.x Operating System.", "8beb8f4b-cf59-4f2c-81b5-74041d379fc6")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint13", "The Linux Mint 13.x Operating System.", "f204c30e-46be-49d3-82fd-7f455e1d3ac3")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint14", "The Linux Mint 14.x Operating System.", "c4ea3a0d-8bdc-48e0-b985-719453831834")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint15", "The Linux Mint 15.x Operating System.", "4b5a4f58-6f90-404f-a7bf-dd805506b197")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint16", "The Linux Mint 16.x Operating System.", "5a1a5259-3387-4a43-9329-0b090f2b6473")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Mint17", "The Linux Mint 17.x Operating System.", "b495af93-5b98-4746-ba29-2ec74f530cc6")
                        .setIsa(getFromMap("7d5b895a-a076-491d-b6ca-58eed0d0c960")));
                addToMap(new TypeNode("Rhel", "The Red Hat Enterprise Linux Operating System.", "98741859-5dd5-4c8d-a64a-69598f5ccb2f")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Rhel3", "The Red Hat Enterprise Linux 3.x Operating System.", "200eed01-43fe-4e9f-a740-a1bf50933751")
                        .setIsa(getFromMap("98741859-5dd5-4c8d-a64a-69598f5ccb2f")));
                addToMap(new TypeNode("Rhel4", "The Red Hat Enterprise Linux 4.x Operating System.", "078f7adf-2647-4f35-8742-96a7c59c992b")
                        .setIsa(getFromMap("98741859-5dd5-4c8d-a64a-69598f5ccb2f")));
                addToMap(new TypeNode("Rhel5", "The Red Hat Enterprise Linux 5.x Operating System.", "04202e27-f9ff-4ce0-8418-0d4aaecd6257")
                        .setIsa(getFromMap("98741859-5dd5-4c8d-a64a-69598f5ccb2f")));
                addToMap(new TypeNode("Rhel6", "The Red Hat Enterprise Linux 6.x Operating System.", "c19c8786-1ed0-4fc1-9ee9-7f0d49022d2c")
                        .setIsa(getFromMap("98741859-5dd5-4c8d-a64a-69598f5ccb2f")));
                addToMap(new TypeNode("Rhel7", "The Red Hat Enterprise Linux 7.x Operating System.", "d4fd6992-f34c-473b-9e05-929d7179ff73")
                        .setIsa(getFromMap("98741859-5dd5-4c8d-a64a-69598f5ccb2f")));
                addToMap(new TypeNode("Centos", "The Community Enterprise Operating System.", "c8b320a0-c5fd-43c0-b52e-305f2daccfec")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Centos3", "The Community Enterprise Operating System, version 3.x", "4a43eead-c3a1-4e10-b8c1-b23e0b5b26ef")
                        .setIsa(getFromMap("c8b320a0-c5fd-43c0-b52e-305f2daccfec")));
                addToMap(new TypeNode("Centos4", "The Community Enterprise Operating System, version 4.x", "8e49d933-dcd4-439f-9d10-91cb48a8e2d4")
                        .setIsa(getFromMap("c8b320a0-c5fd-43c0-b52e-305f2daccfec")));
                addToMap(new TypeNode("Centos5", "The Community Enterprise Operating System, version 5.x", "caaee566-d43e-44b8-a59d-a87c37300562")
                        .setIsa(getFromMap("c8b320a0-c5fd-43c0-b52e-305f2daccfec")));
                addToMap(new TypeNode("Centos6", "The Community Enterprise Operating System, version 6.x", "f3122393-1f4f-4916-96d7-a64b8be5736b")
                        .setIsa(getFromMap("c8b320a0-c5fd-43c0-b52e-305f2daccfec")));
                addToMap(new TypeNode("Centos7", "The Community Enterprise Operating System, version 7.x", "9c13f597-4cd8-4311-a884-6efe24f10c81")
                        .setIsa(getFromMap("c8b320a0-c5fd-43c0-b52e-305f2daccfec")));
                addToMap(new TypeNode("Fedora", "The Fedora Linux Operating System.", "3f90173a-ae4d-4fd5-8c80-5659262d3aef")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Fedora12", "The Fedora 12 Linux Operating System.", "342aecd0-163c-48fa-b0da-8e25b2c27a06")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora13", "The Fedora 13 Linux Operating System.", "f536b385-373e-45d2-9fd5-a41d5171cc45")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora14", "The Fedora 14 Linux Operating System.", "68b7effb-35c0-4b0d-adce-4ea0af5b0c97")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora15", "The Fedora 15 Linux Operating System.", "193c78d5-ad8f-48a4-ac10-8ab775cc7eb9")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora16", "The Fedora 16 Linux Operating System.", "f25ebc43-5f20-469f-90fe-8b907258f648")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora17", "The Fedora 17 Linux Operating System.", "e279d091-8dbe-45af-a41b-02f64800844f")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora18", "The Fedora 18 Linux Operating System.", "ef0cc490-f3cf-48f9-be47-a7ce9a2618db")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora19", "The Fedora 19 Linux Operating System.", "725c35f8-fbf0-442d-a4ec-0d83304e1b2a")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora20", "The Fedora 20 Linux Operating System.", "ca39cf3a-43b2-46b9-8440-cf350c730d29")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Fedora21", "The Fedora 21 Linux Operating System.", "45284c82-2da0-48ae-a698-5f4f707fab94")
                        .setIsa(getFromMap("3f90173a-ae4d-4fd5-8c80-5659262d3aef")));
                addToMap(new TypeNode("Mageia", "The Mageia Linux Operating System.", "26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Mageia3", "The Mageia 3.x Linux Operating System.", "43a6757b-5fc1-4781-9521-b028af50defc")
                        .setIsa(getFromMap("26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c")));
                addToMap(new TypeNode("Mageia4", "The Mageia 4.x Linux Operating System.", "5a516ba5-2d9d-40bb-ba45-c79f630f1953")
                        .setIsa(getFromMap("26b0d6c8-4d6b-4ec2-8d15-6e71758cc06c")));
                addToMap(new TypeNode("OpenSuse", "The openSUSE Linux Operating System.", "cdabdf0d-5994-43ce-803b-c4686ed16f48")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("OpenSuse10", "The openSUSE 10.x Linux Operating System.", "aa0c5261-b3cd-49b8-adf1-243087416064")
                        .setIsa(getFromMap("cdabdf0d-5994-43ce-803b-c4686ed16f48")));
                addToMap(new TypeNode("OpenSuse11", "The openSUSE 11.x Linux Operating System.", "dbe9b27e-5436-4b5b-a851-f0ba0cb04d69")
                        .setIsa(getFromMap("cdabdf0d-5994-43ce-803b-c4686ed16f48")));
                addToMap(new TypeNode("OpenSuse12", "The openSUSE 12.x Linux Operating System.", "90fa3322-60c3-4808-9d83-3af9b5a69b62")
                        .setIsa(getFromMap("cdabdf0d-5994-43ce-803b-c4686ed16f48")));
                addToMap(new TypeNode("OpenSuse13", "The openSUSE 13.x Linux Operating System.", "09ad6070-a602-4e00-ba9c-25a181f7f0df")
                        .setIsa(getFromMap("cdabdf0d-5994-43ce-803b-c4686ed16f48")));
                addToMap(new TypeNode("Sles", "The SUSE Linux Enterprise Server Operating System.", "6cbfc14e-6835-43bc-a29d-aafc8589de4b")
                        .setIsa(getFromMap("a5613527-2f64-4481-83f2-80b0cb427e28")));
                addToMap(new TypeNode("Sles9", "The SUSE Linux Enterprise Server 9.x Operating System.", "019b3997-f28e-4b81-8e5c-b1077fdf86cb")
                        .setIsa(getFromMap("6cbfc14e-6835-43bc-a29d-aafc8589de4b")));
                addToMap(new TypeNode("Sles10", "The SUSE Linux Enterprise Server 10.x Operating System.", "3f1076ed-dbe7-4ca6-8138-5a909bef9ce8")
                        .setIsa(getFromMap("6cbfc14e-6835-43bc-a29d-aafc8589de4b")));
                addToMap(new TypeNode("Sles11", "The SUSE Linux Enterprise Server 11.x Operating System.", "db95db3d-fe10-448a-9f30-2a05bf06e699")
                        .setIsa(getFromMap("6cbfc14e-6835-43bc-a29d-aafc8589de4b")));
                addToMap(new TypeNode("Sles12", "The SUSE Linux Enterprise Server 12.x Operating System.", "e3c6338f-ece3-448e-867b-49a8c16c9974")
                        .setIsa(getFromMap("6cbfc14e-6835-43bc-a29d-aafc8589de4b")));
        }
}
