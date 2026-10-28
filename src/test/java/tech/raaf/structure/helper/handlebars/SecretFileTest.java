package tech.raaf.structure.helper.handlebars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.HandlebarsException;
import com.github.jknack.handlebars.Template;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SecretFileTest {

  @TempDir
  Path folder;

  @Test
  public void writesContentThatOnlyTheOwnerCanRead() throws Exception {
    final List<Path> files = new ArrayList<>();
    final SecretFile secretFile = new SecretFile(folder.resolve("scripts"), files);
    final String command = new Handlebars()
      .with(value -> value.toString())
      .registerHelper("secretFile", secretFile)
      .compileInline(
        "smbclient -A {{#secretFile}}username = {{username}}\npassword = {{password}}\n{{/secretFile}} //host"
      )
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

  @Test
  public void refusesValueWithLineBreak() throws Exception {
    final List<Path> files = new ArrayList<>();
    final SecretFile secretFile = new SecretFile(folder.resolve("scripts"), files);
    final Template template = new Handlebars()
      .with(value -> value.toString())
      .registerHelper("secretFile", secretFile)
      .compileInline("{{#secretFile}}username = {{username}}\npassword = {{password}}\n{{/secretFile}}");
    try {
      template.apply(ImmutableMap.of("username", "Administrator\ndomain = OTHER", "password", "secret"));
      fail("A value with a line break must not go to a secret file");
    } catch (HandlebarsException e) {
      assertTrue(e.getCause() instanceof IllegalArgumentException);
    }
    assertTrue(files.isEmpty());
  }
}
