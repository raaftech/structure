package tech.raaf.structure.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import tech.raaf.structure.base.Configuration;
import tech.raaf.structure.base.Configuration.Setting;

public class ProcessRunnerTest {

  @Test
  public void runsInStateDir() throws Exception {
    final ProcessRunner runner = new ProcessRunner("pwd");
    runner.run();
    while (runner.isRunning()) {
      Thread.sleep(10);
    }
    assertEquals(Configuration.get(Setting.STATE_DIR).toString(), runner.finish().stdout.get().trim());
  }
}
