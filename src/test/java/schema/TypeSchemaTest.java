package schema;

import com.google.common.collect.ImmutableList;
import com.raaftech.structure.base.Configuration;
import com.raaftech.structure.base.Context;
import com.raaftech.structure.base.SpringTestConfig;
import com.raaftech.structure.bus.manager.SchemaManager;
import com.raaftech.structure.helper.loader.BundleLoader;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public abstract class TypeSchemaTest {

    @Autowired
    GraphDatabase graphDatabase;

    private static boolean httpUp = false;
    private static boolean databaseCreated = false;

    /**
     * Generate a random number that can be used as a port. The result is guaranteed
     * not to be a privileged port number.
     * @return An unprivileged random port number.
     */
    private int generateRandomUnprivilegedPort() {
        final int privilegedPorts = 1024;
        final int amountOfPorts = (int) Math.pow(2, 16);
        Random random = new Random();
        /*
        NextInt returns an int from 0 inclusive to arg exclusive. We want to subtract
        the amount of privileged ports from the possible range and add it to the generated
        number.
        The result of this is at least 1024 (which is the last privileged number) and at most
        2**16-1. So we'll add one to that number so we have the complete range of unprivileged
        port numbers.
         */

        return random.nextInt(amountOfPorts - privilegedPorts) + privilegedPorts + 1 ;
    }

    @Before
    public void setUpDatabase() throws Exception {

        // Setup the http endpoint for querying type schema's
        if (!httpUp) {
            // Generate a random port number for the schema http namespace
            final int httpdPort = generateRandomUnprivilegedPort();

            // Setup vertx and http+router
            final Vertx vertx = Vertx.vertx();
            Context.setVertx(vertx);
            final HttpServer httpServer = vertx.createHttpServer();
            final Router router = Router.router(vertx);
            SchemaManager schemaManager = new SchemaManager();
            router.route("/schemas/:schema").handler(schemaManager::getSchema);
            httpServer.requestHandler(router::accept).listen(httpdPort);

            // Set the schema namespace to the http endpoint that we exposed before
            Configuration.setSchemaNamespace(URI.create("http://localhost:" + httpdPort));

            httpUp = true;
        }

        if (!databaseCreated) {
            // The location of the bundle to test
            final Path testBundle = Paths.get("src/main/resources/bundles/");

            // Load the types first
            Transaction tx = graphDatabase.beginTx();
            BundleLoader bundleLoader = new BundleLoader(ImmutableList.of(testBundle));
            bundleLoader.loadTypes();
            tx.success();
            tx.close();

            // Then load the instances
            tx = graphDatabase.beginTx();
            bundleLoader.loadInstances();
            tx.success();
            tx.close();

            databaseCreated = true;
        }
    }

}
