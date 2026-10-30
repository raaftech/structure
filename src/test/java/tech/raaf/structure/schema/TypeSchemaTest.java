package tech.raaf.structure.schema;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import tech.raaf.structure.GraphTest;
import tech.raaf.structure.base.Application;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.SchemaManager;
import tech.raaf.structure.helper.loader.BundleLoader;

/**
 * Loads the bundles before each test, and serves the type schemas.
 */
public abstract class TypeSchemaTest extends GraphTest {

  // The schema server is the same for each test, so start it one time
  private static Vertx vertx;
  private static URI schemaNamespace;
  // The bundles are the same for each test, so read them one time
  private static BundleLoader bundleLoader;

  @BeforeEach
  public void setUpDatabase() throws Exception {
    // Setup the http endpoint for querying type schema's
    if (vertx == null) {
      // Setup vertx and http+router
      vertx = Application.createVertx();
      // Bus actors can wait for HTTP schema requests on their event loop.
      final Vertx schemaVertx = Application.createVertx();
      final HttpServer httpServer = schemaVertx.createHttpServer();
      final Router router = Router.router(schemaVertx);
      SchemaManager schemaManager = new SchemaManager();
      router.route("/schemas/:schema").blockingHandler(schemaManager::getSchema, false);

      // Port 0 lets the system choose a free port
      final HttpServer listening = httpServer.requestHandler(router).listen(0, "localhost").await();

      // The schema namespace is the http endpoint that we exposed before
      schemaNamespace = URI.create("http://localhost:" + listening.actualPort());
    }
    // Other tests in the same JVM can change these, so set them each time
    Context.setVertx(vertx);
    Context.setSchemaNamespace(schemaNamespace);

    // The location of the bundle to test
    if (bundleLoader == null) {
      final Path testBundle = Paths.get("src/main/resources/bundles/");
      bundleLoader = new BundleLoader(testBundle);
    }
    bundleLoader.load();
  }
}
