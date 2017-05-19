package schema;

import com.raaftech.structure.base.Configuration;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = SpringTestConfig.class)
public abstract class TypeSchemaTest {

    @Autowired
    GraphDatabase graphDatabase;

    private static boolean httpUp = false;
    private static boolean databaseCreated = false;

    @Before
    public void setUpDatabase() throws Exception {

        // Setup the http endpoint for querying type schema's
        if (!httpUp) {
            final Vertx vertx = Vertx.vertx();
            final HttpServer httpServer = vertx.createHttpServer();
            final Router router = Router.router(vertx);
            SchemaManager schemaManager = new SchemaManager();
            router.route("/schemas/:schema").handler(schemaManager::getSchema);
            httpServer.requestHandler(router::accept).listen(8080);

            httpUp = true;
        }

        if (!databaseCreated) {
            // The location of the bundle to test
            final String testBundle = "src/main/resources/bundles/";
            // Set the schema namespace to the http endpoint that we exposed before
            Configuration.setSchemaNamespace(URI.create("http://localhost:8080"));


            // Load the types first
            Transaction tx = graphDatabase.beginTx();
            BundleLoader bundleLoader = new BundleLoader(testBundle);
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
