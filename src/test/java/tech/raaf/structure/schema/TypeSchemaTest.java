package tech.raaf.structure.schema;

import com.google.common.collect.ImmutableSet;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import tech.raaf.structure.GraphTest;
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

  @Before
  public void setUpDatabase() throws Exception {
    // Setup the http endpoint for querying type schema's
    if (vertx == null) {
      // Setup vertx and http+router
      vertx = Vertx.vertx();
      final HttpServer httpServer = vertx.createHttpServer();
      final Router router = Router.router(vertx);
      SchemaManager schemaManager = new SchemaManager();
      router.route("/schemas/:schema").blockingHandler(schemaManager::getSchema, false);

      // Port 0 lets the system choose a free port
      final CompletableFuture<HttpServer> listening = new CompletableFuture<>();
      httpServer.requestHandler(router::accept).listen(0, "localhost", ar -> {
        if (ar.succeeded()) {
          listening.complete(ar.result());
        } else {
          listening.completeExceptionally(ar.cause());
        }
      });

      // The schema namespace is the http endpoint that we exposed before
      schemaNamespace = URI.create("http://localhost:" + listening.get().actualPort());
    }
    // Other tests in the same JVM can change these, so set them each time
    Context.setVertx(vertx);
    Context.setSchemaNamespace(schemaNamespace);

    // The location of the bundle to test
    if (bundleLoader == null) {
      final Path testBundle = Paths.get("src/main/resources/bundles/");
      bundleLoader = new BundleLoader(ImmutableSet.of(testBundle));
    }
    bundleLoader.load();
  }
}
