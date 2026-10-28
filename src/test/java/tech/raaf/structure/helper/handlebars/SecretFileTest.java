package tech.raaf.structure.helper.handlebars;

import static org.junit.Assert.assertEquals;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SecretFileTest {

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  @Test
  public void writesContentThatOnlyTheOwnerCanRead() throws Exception {
    final List<Path> files = new ArrayList<>();
    final SecretFile secretFile = new SecretFile(folder.getRoot().toPath().resolve("scripts"), files);
    final String command = new Handlebars()
      .with(value -> value.toString())
      .registerHelper("secretFile", secretFile)
      .compileInline("smbclient -A {{#secretFile}}username = {{username}}\npassword = {{password}}\n{{/secretFile}} //host")
      .apply(ImmutableMap.of("username", "Administrator", "password", "p@ss 'word'%=&"));

    assertEquals(1, files.size());
    final Path file = files.get(0);
    assertEquals("smbclient -A '" + file + "' //host", command);
    assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(file)));
    assertEquals(
      "username = Administrator\npassword = p@ss 'word'%=&\n",
      new String(Files.readAllBytes(file), StandardCharsets.UTF_8)
    );
  }
}
