package tech.raaf.structure.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class KeystoreHelperTest {

  @TempDir
  Path folder;

  // The subject alternative names of the exported certificate.
  private List<List<?>> names(Path jks) throws Exception {
    try (InputStream stream = Files.newInputStream(KeystoreHelper.crt(jks))) {
      final X509Certificate crt = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(stream);
      return new ArrayList<>(crt.getSubjectAlternativeNames());
    }
  }

  @Test
  public void make() throws Exception {
    // Also makes the missing directory.
    final Path jks = folder.resolve("target/structure.jks");
    KeystoreHelper.make(jks, "changeit", "structure.example", "rest.example", "localhost", "structure.example");
    assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(jks)));
    assertEquals(
      Arrays.asList(
        Arrays.asList(2, "localhost"),
        Arrays.asList(2, "structure.example"),
        Arrays.asList(2, "rest.example")
      ),
      names(jks)
    );
    // The exported key is the key in the keystore.
    final Path key = KeystoreHelper.key(jks);
    assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(key)));
    final String pem = new String(Files.readAllBytes(key), "US-ASCII");
    final KeyStore keystore = KeyStore.getInstance("JKS");
    try (InputStream stream = Files.newInputStream(jks)) {
      keystore.load(stream, "changeit".toCharArray());
    }
    assertArrayEquals(
      keystore.getKey("structure", "changeit".toCharArray()).getEncoded(),
      Base64.getMimeDecoder().decode(pem.replaceAll("-----[A-Z ]+-----", ""))
    );
  }

  @Test
  public void makeForAddress() throws Exception {
    final Path jks = folder.resolve("structure.jks");
    KeystoreHelper.make(jks, "changeit", "10.0.0.1");
    assertEquals(Arrays.asList(Arrays.asList(2, "localhost"), Arrays.asList(7, "10.0.0.1")), names(jks));
  }

  @Test
  public void trust() throws Exception {
    final Path jks = folder.resolve("structure.jks");
    KeystoreHelper.make(jks, "changeit", "localhost");
    KeystoreHelper.trust(jks, "changeit");
    final Vertx vertx = Vertx.vertx();
    try {
      final HttpServer listening = vertx
        .createHttpServer(
          new HttpServerOptions()
            .setSsl(true)
            .setKeyCertOptions(new JksOptions().setPath(jks.toString()).setPassword("changeit"))
        )
        .requestHandler(request -> request.response().end("ok"))
        .listen(0, "localhost")
        .await();
      // The default trust and host name check accept the certificate.
      final URL url = URI.create("https://localhost:" + listening.actualPort() + "/").toURL();
      try (InputStream stream = url.openStream()) {
        assertEquals("ok", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
      }
    } finally {
      vertx.close();
    }
  }
}
