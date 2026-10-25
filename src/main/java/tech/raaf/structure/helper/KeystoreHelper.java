package tech.raaf.structure.helper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.io.IOUtils;

/**
 * Makes the keystore of the HTTPS server, and makes the JVM trust its
 * certificates together with the certificates of the JDK.
 */
public final class KeystoreHelper {

  // The alias of the key in the keystore that make creates
  private static final String ALIAS = "structure";
  // The environment variable that gives keytool the password
  private static final String PASS = "KEYTOOL_PASS";

  private KeystoreHelper() {}

  /**
   * Make a JKS keystore with a self-signed certificate for localhost and
   * the given hosts. Export the certificate to structure.crt and the key
   * to structure.key, in the directory of the keystore. Only the owner
   * can read the keystore and the key.
   */
  public static void make(Path jks, String password, String... hosts)
    throws IOException, InterruptedException, GeneralSecurityException {
    Files.createDirectories(jks.toAbsolutePath().getParent());
    final StringBuilder san = new StringBuilder("dns:localhost");
    for (String host : new LinkedHashSet<>(Arrays.asList(hosts))) {
      if (!"localhost".equals(host)) {
        san.append(host.matches("[\\d.]+|.*:.*") ? ",ip:" : ",dns:").append(host);
      }
    }
    keytool(
      password,
      "-genkeypair",
      "-alias",
      ALIAS,
      "-keyalg",
      "RSA",
      "-validity",
      "3650",
      "-dname",
      "CN=localhost",
      "-ext",
      "san=" + san,
      "-storetype",
      "JKS",
      "-keystore",
      jks.toString(),
      "-storepass:env",
      PASS,
      "-keypass:env",
      PASS
    );
    Files.setPosixFilePermissions(jks, PosixFilePermissions.fromString("rw-------"));
    final Key key = load(jks, password.toCharArray()).getKey(ALIAS, password.toCharArray());
    final String pem = Base64.getMimeEncoder(64, new byte[] { '\n' }).encodeToString(key.getEncoded());
    Files.createFile(key(jks), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
    Files.write(
      key(jks),
      ("-----BEGIN PRIVATE KEY-----\n" + pem + "\n-----END PRIVATE KEY-----\n").getBytes("US-ASCII")
    );
    keytool(
      password,
      "-exportcert",
      "-rfc",
      "-alias",
      ALIAS,
      "-keystore",
      jks.toString(),
      "-storepass:env",
      PASS,
      "-file",
      crt(jks).toString()
    );
  }

  // The certificate file that make exports
  public static Path crt(Path jks) {
    return jks.toAbsolutePath().resolveSibling("structure.crt");
  }

  // The key file that make exports
  public static Path key(Path jks) {
    return jks.toAbsolutePath().resolveSibling("structure.key");
  }

  /**
   * Set the trust of the default SSLContext to the certificates of the
   * JDK and the certificates in the keystore.
   */
  public static void trust(Path jks, String password) throws IOException, GeneralSecurityException {
    final KeyStore trusted = load(Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts"), null);
    final KeyStore keystore = load(jks, password.toCharArray());
    for (String alias : Collections.list(keystore.aliases())) {
      trusted.setCertificateEntry(ALIAS + "-" + alias, keystore.getCertificate(alias));
    }
    final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    factory.init(trusted);
    final SSLContext context = SSLContext.getInstance("TLS");
    context.init(null, factory.getTrustManagers(), null);
    SSLContext.setDefault(context);
  }

  private static KeyStore load(Path file, char[] password) throws IOException, GeneralSecurityException {
    final KeyStore store = KeyStore.getInstance("JKS");
    try (InputStream stream = Files.newInputStream(file)) {
      store.load(stream, password);
    }
    return store;
  }

  // Run the keytool of this JVM, with the password in its environment
  private static void keytool(String password, String... args) throws IOException, InterruptedException {
    final List<String> command = new ArrayList<>();
    command.add(Paths.get(System.getProperty("java.home"), "bin", "keytool").toString());
    command.addAll(Arrays.asList(args));
    final ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
    builder.environment().put(PASS, password);
    final Process process = builder.start();
    // No input, so keytool fails if it prompts
    process.getOutputStream().close();
    final String output = IOUtils.toString(process.getInputStream());
    if (process.waitFor() != 0) {
      throw new IOException("keytool " + args[0] + " failed: " + output.trim());
    }
  }
}
