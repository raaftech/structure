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

  @Test
  public void passesStateDirAndCertificateToTools() throws Exception {
    ProcessRunner runner = new ProcessRunner("printenv STRUCTURE_STATE_DIR NODE_EXTRA_CA_CERTS");
    runner.run();
    assertEquals(0, runner.getProcess().waitFor());
    assertEquals(
      Configuration.get(Setting.STATE_DIR) + "\n" + KeystoreHelper.crt(Configuration.get(Setting.JKS_FILE)),
      runner.finish().stdout.get().trim()
    );
  }
}
