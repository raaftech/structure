#!/usr/bin/env bash

if [[ -z "$NEO4J_USERNAME" || -z "$NEO4J_PASSWORD" ]]; then
  echo "Error: variables NEO4J_USERNAME and/or NEO4J_PASSWORD are not set"
  exit 1
fi

echo "Warn: will delete everything and load a clean data model.."
echo "Press CTRL-C to abort within 10 seconds.."


echo "Cleaning: Graph Database.."
cypher-shell 'MATCH (x) DETACH DELETE x'

echo "Deleting: UUID Contraint.."
cypher-shell 'DROP CONSTRAINT ObjectUuid'

echo "Loading: OpenPCF Base Model.."
cypher-shell < org.openpcf.model.base.cql

echo "Loading: OpenPCF CloudSystems.."
cypher-shell < org.openpcf.model.cloudsystems.cql

echo "Loading: OpenPCF Emulators.."
cypher-shell < org.openpcf.model.emulators.cql

echo "Loading: OpenPCF Hypervisors.."
cypher-shell < org.openpcf.model.hypervisors.cql

echo "Loading: OpenPCF Interpreters.."
cypher-shell < org.openpcf.model.interpreters.cql

echo "Loading: OpenPCF Operating Systems.."
cypher-shell < org.openpcf.model.operatingsystems.cql

echo "Loading: OpenPCF Package Managers.."
cypher-shell < org.openpcf.model.packagemanagers.cql

echo "Loading: OpenPCF Service Managers.."
cypher-shell < org.openpcf.model.servicemanagers.cql

echo "Loading: OpenPCF Services.."
cypher-shell < org.openpcf.model.services.cql

echo "Loading: RAAF CloudSystems.."
cypher-shell < com.raaftech.model.cloudsystems.cql

echo "Loading: RAAF Emulators.."
cypher-shell < com.raaftech.model.emulators.cql

echo "Loading: RAAF Hypervisors.."
cypher-shell < com.raaftech.model.hypervisors.cql

echo "Loading: RAAF Interpreters.."
cypher-shell < com.raaftech.model.interpreters.cql

echo "Loading: RAAF Operating Systems.."
cypher-shell < com.raaftech.model.operatingsystems.cql

echo "Loading: RAAF Package Managers.."
cypher-shell < com.raaftech.model.packagemanagers.cql

echo "Loading: RAAF Service Managers.."
cypher-shell < com.raaftech.model.servicemanagers.cql

echo "Loading: RAAF Services.."
cypher-shell < com.raaftech.model.services.cql

echo -e "\nDone!"
