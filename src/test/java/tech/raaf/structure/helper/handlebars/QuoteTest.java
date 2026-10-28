package tech.raaf.structure.helper.handlebars;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class QuoteTest {

  // Characters that a shell can change in a word, also in quotes
  private static final String VALUE = "it's! x!! \\! $HOME `id` ~ * {a,b} ^a^b #c \"d\" a\nb a\\\nb \\";

  @Test
  public void shQuotesEachCharacter() {
    assertEquals("'a b'", Quote.SH.quote("a b"));
    assertEquals("'it'\"'\"'s'", Quote.SH.quote("it's"));
    assertEquals("'$(id); `id` \"x\" \\'", Quote.SH.quote("$(id); `id` \"x\" \\"));
    assertEquals("''", Quote.SH.quote(""));
  }

  @Test
  public void cshEscapesHistoryAndLineBreak() {
    assertEquals("'a\\!b'", Quote.CSH.quote("a!b"));
    assertEquals("'a\\\nb'", Quote.CSH.quote("a\nb"));
    assertEquals("'it'\"'\"'s'", Quote.CSH.quote("it's"));
  }

  @Test
  public void shellOfExtension() {
    assertEquals(Quote.CSH, Quote.forShell("csh"));
    assertEquals(Quote.SH, Quote.forShell("sh"));
    assertEquals(Quote.SH, Quote.forShell(""));
  }

  @Test
  public void shScriptGetsValueUnchanged() throws Exception {
    assertEquals("[" + VALUE + "]", runScript("/bin/sh", Quote.SH));
  }

  @Test
  public void cshScriptGetsValueUnchanged() throws Exception {
    assertEquals("[" + VALUE + "]", runScript("/bin/csh", Quote.CSH));
  }

  @Test
  public void tcshScriptGetsValueUnchanged() throws Exception {
    assertEquals("[" + VALUE + "]", runScript("/bin/tcsh", Quote.CSH));
  }

  @Test
  public void cmdQuotesPercentAndTrailingBackslash() {
    assertEquals("\"a & b | c\"", Quote.CMD.quote("a & b | c"));
    assertEquals("\"100%%\"", Quote.CMD.quote("100%"));
    assertEquals("\"C:\\dir\\\\\"", Quote.CMD.quote("C:\\dir\\"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void cmdRejectsDoubleQuote() {
    Quote.CMD.quote("a\" & calc & \"");
  }

  @Test(expected = IllegalArgumentException.class)
  public void cmdRejectsLineBreak() {
    Quote.CMD.quote("a\r\ncalc");
  }

  @Test
  public void helperQuotesValues() throws Exception {
    final Handlebars handlebars = new Handlebars().registerHelper("sh", Quote.SH);
    assertEquals(
      "kill 'a&b=c' ''",
      handlebars.compileInline("kill {{sh name}} {{sh missing}}").apply(ImmutableMap.of("name", "a&b=c"))
    );
  }

  // Run a script with one quoted value, as an Invoke runs it
  private static String runScript(String shell, Quote quote) throws Exception {
    assumeTrue(shell + " is not installed", Files.isExecutable(Paths.get(shell)));
    final Path script = Files.createTempFile("quote", ".script");
    try {
      Files.write(script, ("printf '[%s]' " + quote.quote(VALUE) + "\n").getBytes(StandardCharsets.UTF_8));
      final Process process = new ProcessBuilder(shell, script.toString()).redirectErrorStream(true).start();
      final String output = new String(ByteStreams.toByteArray(process.getInputStream()), StandardCharsets.UTF_8);
      process.waitFor();
      return output;
    } finally {
      Files.delete(script);
    }
  }
}
