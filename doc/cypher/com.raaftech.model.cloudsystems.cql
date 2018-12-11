// com.raaftech.model.cloudsystems-0.0.1


// CloudSystemModel 0.0.1 - Main cloudsystems.
MERGE (raafcloudmodel:Object:Model:CloudSystemModel {
    uuid: '3b04dee1-1015-4355-96aa-df7129ac54a5',
    name: 'CloudSystemModel',
    desc: 'Extended cloud (operating) system types.',
    ver: '0.0.1',
    ns: 'com.raaftech.model.cloudsystems'
    });

MATCH (raafcloudmodel:CloudSystemModel {uuid: '3b04dee1-1015-4355-96aa-df7129ac54a5'}),
    (model:Model {uuid: '4e2dccfa-b5ca-4992-a76e-c390b7d90084'})
MERGE (raafcloudmodel)-[:ISA]->(model);

MATCH (raafcloudmodel:CloudSystemModel {uuid: '3b04dee1-1015-4355-96aa-df7129ac54a5'}),
    (opcfcloudmodel:CloudSystemModel {uuid: '4a8e288a-696d-4616-bb77-970dcf3e150d'})
MERGE (raafcloudmodel)-[:REQUIRES]->(opcfcloudmodel);


// AmazonWebServices
MERGE (aws:Object:Component:Software:CloudSystem:AmazonWebServices {
    uuid: '4cd84c58-3cb3-41c1-89d8-6c2fa70ddb54',
    name: 'AmazonWebServices',
    desc: 'The Amazon Web Services Cloud platform.',
    shortName: 'aws',
    longName: 'Amazon Web Services'
    });

MATCH (aws:AmazonWebServices {uuid: '4cd84c58-3cb3-41c1-89d8-6c2fa70ddb54'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (aws)-[:ISA]->(cloudsystem);


// IbmBlueMix
MERGE (bluemix:Object:Component:Software:CloudSystem:IbmBlueMix {
    uuid: '7e60d3b9-5837-41c3-bd68-12c850171441',
    name: 'IbmBlueMix',
    desc: 'The IBM BlueMix Cloud platform.',
    shortName: 'bluemix',
    longName: 'IBM BlueMix'
    });

MATCH (bluemix:IbmBlueMix {uuid: '7e60d3b9-5837-41c3-bd68-12c850171441'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (bluemix)-[:ISA]->(cloudsystem);


// CitrixCloudCom
MERGE (cloudcom:Object:Component:Software:CloudSystem:CitrixCloudCom {
    uuid: 'a5fafd65-2ffc-4509-8289-5760cd15a293',
    name: 'CitrixCloudCom',
    desc: 'The Citrix CLoud.com platform.',
    shortName: 'cloudcom',
    longName: 'Citrix Cloud.com'
    });

MATCH (cloudcom:CitrixCloudCom {uuid: 'a5fafd65-2ffc-4509-8289-5760cd15a293'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (cloudcom)-[:ISA]->(cloudsystem);


// GoogleCloud
MERGE (googlecloud:Object:Component:Software:CloudSystem:GoogleCloud {
    uuid: '0f48c798-6621-4659-9f13-a37e54ef4568',
    name: 'GoogleCloud',
    desc: 'The Google Cloud platform.',
    shortName: 'googlecloud',
    longName: 'Google Cloud'
    });

MATCH (googlecloud:GoogleCloud {uuid: '0f48c798-6621-4659-9f13-a37e54ef4568'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (googlecloud)-[:ISA]->(cloudsystem);


// HerokuCloud
MERGE (heroku:Object:Component:Software:CloudSystem:HerokuCloud {
    uuid: '26a6929f-ef61-4714-baa0-e72bdbf5960a',
    name: 'HerokuCloud',
    desc: 'The Heroku Cloud platform.',
    shortName: 'heroku',
    longName: 'Heroku Cloud'
    });

MATCH (heroku:HerokuCloud {uuid: '26a6929f-ef61-4714-baa0-e72bdbf5960a'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (heroku)-[:ISA]->(cloudsystem);


// HpCloud
MERGE (hpcloud:Object:Component:Software:CloudSystem:HpCloud {
    uuid: 'e7806e74-9002-4f49-9e87-9adf25490efb',
    name: 'HpCloud',
    desc: 'The Heroku Cloud platform.',
    shortName: 'hpcloud',
    longName: 'Heroku Cloud'
    });

MATCH (hpcloud:HpCloud {uuid: 'e7806e74-9002-4f49-9e87-9adf25490efb'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (hpcloud)-[:ISA]->(cloudsystem);


// JoyentCloud
MERGE (joyent:Object:Component:Software:CloudSystem:JoyentCloud {
    uuid: 'c60297ae-9fc8-4567-a272-7e2b439b4c57',
    name: 'JoyentCloud',
    desc: 'The Joyent Cloud platform.',
    shortName: 'joyent',
    longName: 'Joyent Cloud'
    });

MATCH (joyent:JoyentCloud {uuid: 'c60297ae-9fc8-4567-a272-7e2b439b4c57'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (joyent)-[:ISA]->(cloudsystem);


// MicrosoftAzure
MERGE (azure:Object:Component:Software:CloudSystem:MicrosoftAzure {
    uuid: '171c2c6a-9101-437b-b464-e99d276ded47',
    name: 'MicrosoftAzure',
    desc: 'The Microsoft Azure Cloud platform.',
    shortName: 'azure',
    longName: 'Microsoft Azure'
    });

MATCH (azure:MicrosoftAzure {uuid: '171c2c6a-9101-437b-b464-e99d276ded47'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (azure)-[:ISA]->(cloudsystem);


// OnAppCloud
MERGE (onapp:Object:Component:Software:CloudSystem:OnAppCloud {
    uuid: '3f368aba-b0f2-4e14-9d7b-715312a7e385',
    name: 'OnAppCloud',
    desc: 'The OnApp Cloud platform.',
    shortName: 'onapp',
    longName: 'OnApp Cloud'
    });

MATCH (onapp:OnAppCloud {uuid: '3f368aba-b0f2-4e14-9d7b-715312a7e385'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (onapp)-[:ISA]->(cloudsystem);


// OracleCloud
MERGE (oraclecloud:Object:Component:Software:CloudSystem:OracleCloud {
    uuid: 'fc070f26-87a0-45e8-8ae0-e894fcdb83ea',
    name: 'OracleCloud',
    desc: 'The Oracle Cloud platform.',
    shortName: 'oraclecloud',
    longName: 'Oracle Cloud'
    });

MATCH (oraclecloud:OracleCloud {uuid: 'fc070f26-87a0-45e8-8ae0-e894fcdb83ea'}),
    (cloudsystem:CloudSystem {uuid: '86c0439a-4219-46e6-82a3-6fb2ee774aa4'})
MERGE (oraclecloud)-[:ISA]->(cloudsystem);
