package tech.raaf.structure.helper.handlebars;

import static org.junit.Assert.assertEquals;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

public class QuoteTest {

  @Test
  public void shQuotesEachCharacter() {
    assertEquals("'a b'", Quote.SH.quote("a b"));
    assertEquals("'it'\"'\"'s'", Quote.SH.quote("it's"));
    assertEquals("'$(id); `id` \"x\" \\'", Quote.SH.quote("$(id); `id` \"x\" \\"));
    assertEquals("''", Quote.SH.quote(""));
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
}
