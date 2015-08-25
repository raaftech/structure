import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.raaftech.structure.base.Application;
import com.raaftech.structure.helper.HandlerPlugin;

public class Plugin extends HandlerPlugin {

    final static Logger log = LoggerFactory.getLogger(Plugin.class);

    private String privateKey;
    private String strictHostKeyChecking;

    private class ExecResult {
        String stdout;
        String stderr;
        int returnCode;

        public ExecResult(String stdout, String stderr, int returnCode) {
            this.stdout = stdout;
            this.stderr = stderr;
            this.returnCode = returnCode;
        }
    }

    public void implementation() {
        privateKey = metadata.getString("path") + "/id_rsa";
        String command = args.get("command").asText();
        String address = args.get("address").asText();

        ExecResult result = executeSSHCommand(command, address);

        if (result != null) {
            pluginResult.put("stdout", result.stdout);
            pluginResult.put("stderr", result.stderr);
            pluginResult.put("returncode", result.returnCode);
        }
    }

    private ExecResult executeSSHCommand(String command, String address) {
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;
        ExecResult result = null;
        try {
            // TODO: Query atlas for identity
            jsch.addIdentity(privateKey);
            session = jsch.getSession("root", address);
            // TODO: Think about known hosts. Current config is UNSAFE
            jsch.setConfig("StrictHostKeyChecking", "no");
//            jsch.setKnownHosts(metadata.getString("path") + "/known_hosts");

            // TODO: Dynamic based on location?
            session.connect(100);
            channel = session.openChannel("exec");
            // TODO: safety!!!
            ((ChannelExec)channel).setCommand(command);
            InputStream stdout = channel.getInputStream();
            InputStream stderr = ((ChannelExec)channel).getErrStream();

            channel.connect();
            String out = new String();
            while (!channel.isEOF()) {}
            while (stdout.available() > 0) {
                byte[] tmp = new byte[1024];
                int i=stdout.read(tmp, 0, 1024);
                if(i<0)break;
                out += new String(tmp, 0, i);
            }

            String err = new String();
            while (stderr.available() > 0) {
                byte[] tmp = new byte[1024];
                int i=stderr.read(tmp, 0, 1024);
                if(i<0)break;
                err += new String(tmp, 0, i);
            }

            result = new ExecResult(out, err, channel.getExitStatus());

        } catch (JSchException e) {
            pluginResult.put("error", e.getMessage());
            log.error(e.getMessage());
            return null;
        } catch (IOException e) {
            pluginResult.put("error", e.getMessage());
            log.error(e.getMessage());
            return null;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
        return result;
    }
}