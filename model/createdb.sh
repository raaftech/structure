#!/bin/sh

neo4j_home="/opt/neo/neo4j2"
neo4j_exec="$neo4j_home/bin/neo4j"
neo4j_repl="$neo4j_home/bin/neo4j-shell"

"$neo4j_exec" stop

rm -rf "$neo4j_home/data/*"

"$neo4j_exec" start
echo ""

echo "OpenPCF Base Model"
sleep 3
"$neo4j_repl" -file org.openpcf.model.base.cql
echo ""

echo "OpenPCF CloudSystems"
sleep 3
"$neo4j_repl" -file org.openpcf.model.cloudsystems.cql
echo ""

echo "OpenPCF Hypervisors"
sleep 3
"$neo4j_repl" -file org.openpcf.model.hypervisors.cql
echo ""

echo "OpenPCF Interpreters"
sleep 3
"$neo4j_repl" -file org.openpcf.model.interpreters.cql
echo ""

echo "OpenPCF Operating Systems"
sleep 3
"$neo4j_repl" -file org.openpcf.model.operatingsystems.cql
echo ""

echo "OpenPCF Package Managers"
sleep 3
"$neo4j_repl" -file org.openpcf.model.packagemanagers.cql
echo ""

echo "OpenPCF Service Managers"
sleep 3
"$neo4j_repl" -file org.openpcf.model.servicemanagers.cql
echo ""

echo "OpenPCF Services"
sleep 3
"$neo4j_repl" -file org.openpcf.model.services.cql
echo ""

echo Done
echo ""
