package org.openpcf.universal.base;

import org.neo4j.graphdb.Transaction;
import org.openpcf.universal.graph.repositories.NodeRepository;
import org.openpcf.universal.graph.repositories.TypeNodeRepository;
import org.openpcf.universal.graph.schema.SchemaUpdate;
import org.openpcf.universal.graph.schema.base.BaseR1;
import org.openpcf.universal.graph.schema.cloudsystems.CloudSystemsR1;
import org.openpcf.universal.graph.schema.emulators.EmulatorsR1;
import org.openpcf.universal.graph.schema.hypervisors.HypervisorsR1;
import org.openpcf.universal.graph.schema.interpreters.InterpretersR1;
import org.openpcf.universal.graph.schema.operatingsystems.OperatingSystemsR1;
import org.openpcf.universal.graph.schema.packagemanagers.PackageManagersR1;
import org.openpcf.universal.graph.schema.servicemanagers.ServiceManagersR1;
import org.openpcf.universal.graph.schema.services.ServicesR1;
import org.springframework.data.neo4j.core.GraphDatabase;

import java.util.ArrayList;

/**
 * Give a description of SetupDatabase here.
 *
 * @author Boy van Duuren <boy.van.duuren@raaftech.com>
 * @since 2014-11-25
 */
public class SetupDatabase {

    GraphDatabase graphDatabase;
    NodeRepository nodeRepository;
    TypeNodeRepository typeNodeRepository;

    public SetupDatabase() {
        this.graphDatabase = SpringContext.INSTANCE.getGraphDatabase();
        this.nodeRepository = SpringContext.INSTANCE.getNodeRepository();
        this.typeNodeRepository = SpringContext.INSTANCE.getTypeNodeRepository();
    }

    public boolean createDatabase() {
        ArrayList<SchemaUpdate> schemas = new ArrayList<>();
        Transaction tx = graphDatabase.beginTx();

        schemas.add(new BaseR1());
        schemas.add(new CloudSystemsR1());
        schemas.add(new EmulatorsR1());
        schemas.add(new HypervisorsR1());
        schemas.add(new InterpretersR1());
        schemas.add(new OperatingSystemsR1());
        schemas.add(new PackageManagersR1());
        schemas.add(new ServiceManagersR1());
        schemas.add(new ServicesR1());

        try {
            long startTime = System.nanoTime();
            schemas.forEach(schema -> schema.apply());
            long endTime = System.nanoTime();
            System.out.println("Created the TypeNode database in " + (endTime - startTime) / 1000000 + "ms.");
            tx.success();
        } finally {
            tx.close();
        }
        return true;
    }
}
