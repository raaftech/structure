package tech.raaf.structure.helper.handlebars;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

/**
 * Quote can be used to give a value as one word of a command for sh,
 * csh or cmd.
 */
public enum Quote implements Helper<Object> {
  SH {
    @Override
    public String quote(String value) {
      // In single quotes each character is literal, except the quote
      return "'" + value.replace("'", "'\"'\"'") + "'";
    }
  },
  CSH {
    @Override
    public String quote(String value) {
      // In single quotes csh still expands ! and ends at a line break
      return SH.quote(value.replace("!", "\\!").replace("\n", "\\\n"));
    }
  },
  CMD {
    @Override
    public String quote(String value) {
      // cmd cannot escape a double quote or a line break
      if (!value.matches("[^\"\\r\\n]*")) {
        throw new IllegalArgumentException("A value for cmd contains a double quote or a line break.");
      }
      // A script needs %%, and a backslash before the end quote escapes it
      return "\"" + value.replace("%", "%%").replaceAll("(\\\\+)$", "$1$1") + "\"";
    }
  };

  public abstract String quote(String value);

  // The quote for a Unix shell script with the given file extension
  public static Quote forShell(String extension) {
    return "csh".equals(extension) ? CSH : SH;
  }

  @Override
  public CharSequence apply(Object value, Options options) {
    return new Handlebars.SafeString(quote(value == null ? "" : value.toString()));
  }
}
