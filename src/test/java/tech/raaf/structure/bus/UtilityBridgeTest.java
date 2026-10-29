package tech.raaf.structure.bus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.raaf.structure.base.Context;
import tech.raaf.structure.bus.manager.EntityManager;
import tech.raaf.structure.bus.repository.Instances;
import tech.raaf.structure.bus.repository.Types;
import tech.raaf.structure.helper.KeystoreHelper;
import tech.raaf.structure.schema.TypeSchemaTest;

public class UtilityBridgeTest extends TypeSchemaTest {

  @Test
  public void utilitiesUseTheHttpsBridgeWithoutRest(@TempDir Path directory) throws Exception {
    var vertx = Context.getVertx();
    vertx.deployVerticle(new EntityManager(), GenericActor.worker()).await();
    vertx.deployVerticle(new Instances(Context.getInstanceRepository()), GenericActor.worker()).await();
    vertx.deployVerticle(new Types(Context.getTypeRepository()), GenericActor.worker()).await();
    Path jks = directory.resolve("structure.jks");
    KeystoreHelper.make(jks, "changeit", "localhost");
    Router router = Router.router(vertx);
    router.route("/eventbus/*").subRouter(SockJSHandler.create(vertx).bridge(
      new SockJSBridgeOptions()
        .addInboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"))
        .addOutboundPermitted(new PermittedOptions().setAddressRegex("tech\\.raaf\\.structure\\..*"))
    ));
    HttpServer server = vertx.createHttpServer(
      new HttpServerOptions().setSsl(true).setKeyCertOptions(new JksOptions().setPath(jks.toString()).setPassword("changeit"))
    ).requestHandler(router).listen(0, "localhost").await();
    Process process = null;
    Path output = directory.resolve("node.log");
    try {
      int port = server.actualPort();
      Files.writeString(directory.resolve("structure.json"), "{\"https-host\":\"localhost\",\"https-port\":" + port + "}");
      ProcessBuilder builder = new ProcessBuilder("node", "--test", "ext/util/bridge.test.js");
      builder.environment().put("STRUCTURE_STATE_DIR", directory.toString());
      builder.environment().put("NODE_EXTRA_CA_CERTS", KeystoreHelper.crt(jks).toString());
      builder.environment().put("STRUCTURE_TEST_URL", "wss://localhost:" + port + "/eventbus/websocket");
      process = builder.redirectErrorStream(true).redirectOutput(output.toFile()).start();
      assertTrue(process.waitFor(90, TimeUnit.SECONDS), "Node utility tests timed out");
      assertEquals(0, process.exitValue(), Files.readString(output));
    } finally {
      if (process != null && process.isAlive()) process.destroyForcibly();
      server.close().await();
    }
  }
}
