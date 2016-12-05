package io.tchepannou.kribi.aws;

import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.aws.services.Route53;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.services.Installer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CloudFormationInstaller implements Installer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationInstaller.class);

//    private final String username = "ec2-user";
//    private final int port = 22;
    private final AwsContext context;
    private final Route53 route53;
    private final EC2 ec2;

    public CloudFormationInstaller(final AwsContext context) {
        this.context = context;
        this.route53 = new Route53(context);
        this.ec2 = new EC2(context);
    }

    public void install(final DeployRequest deployRequest, final Cluster cluster) {
        ec2.install(deployRequest, cluster);
        final String domainName = route53.deploy(deployRequest, cluster);

        healthCheck(domainName, deployRequest);
    }

//    void install(final DeployRequest deployRequest, final Host host) {
//        try {
//
//            waitForSSH(host);
//
//            final Shell shell = createShell(host);
//
//            copyProfile(deployRequest, shell, host);
//            copyScripts(deployRequest, shell, host);
//            runInstaller(shell, host);
//        } catch (final UnknownHostException e) {
//            throw new KribiException(KribiException.UNEXPECTED_ERROR, host.getPublicIp(), e);
//        }
//    }
//
//    void copyProfile(final DeployRequest deployRequest, final Shell shell, final Host host) {
//        final Application app = deployRequest.getApplication();
//        final Environment env = deployRequest.getEnvironment();
//
//        LOGGER.info("Creating file: {}@{}:~/service-profile", username, host.getPublicIp());
//
//        final List<String> cmds = new ArrayList<>();
//        cmds.add("echo SERVICE_ENVIRONMENT=" + env.name().toLowerCase() + " > service-profile");
//        cmds.add("echo SERVICE_NAME=" + app.getName() + " >> service-profile");
//        cmds.add("echo SERVICE_NAME=" + app.getName() + " >> service-profile");
//        cmds.add("echo SERVICE_USER=" + app.getName() + " >> service-profile");
//        cmds.add("echo SERVICE_VERSION=" + deployRequest.getVersion() + " >> service-profile");
//
//        exec(String.join(";", cmds), shell);
//    }
//
//    void copyScripts(final DeployRequest deployRequest, final Shell shell, final Host host) {
//        final Application app = deployRequest.getApplication();
//
//        LOGGER.info("Copying install scripts to {}@{}", username, host.getPublicIp());
//
//        final String path = "installer/" + app.getTemplate();
//        final URL url = getClass().getClassLoader().getResource(path);
//        if (url == null) {
//            throw new KribiException(KribiException.INSTALL_SCRIPTS_NOT_FOUND, "Unable to locate installer scripts in " + path);
//        }
//
//        final File dir = new File(url.getFile());
//        for (final File f : dir.listFiles()) {
//            scp(f, shell, host);
//        }
//    }
//
//    private void runInstaller(final Shell shell, final Host host) {
//        LOGGER.info("Running installer {}@{}:~/install.sh", username, host.getPublicIp());
//
//        exec("chmod +x install.sh", shell);
//        exec("sudo ./install.sh", shell);
//    }
//
//    void exec(final String cmd, final Shell shell) {
//        try {
//            new Shell.Plain(shell).exec(cmd);
//        } catch (final IOException e) {
//            throw new KribiException(KribiException.SSH_ERROR, "Unable to execute: " + cmd, e);
//        }
//    }
//
//    void scp(final File file, final Shell shell, final Host host) {
//        try (final InputStream in = new FileInputStream(file)) {
//            final ByteArrayOutputStream out = new ByteArrayOutputStream();
//            final ByteArrayOutputStream err = new ByteArrayOutputStream();
//
//            LOGGER.info(".... scp {} {}@{}:~/{}", file.getAbsolutePath(), username, host.getPublicIp(), file.getName());
//            shell.exec(
//                    String.format("cat > %s", file.getName()),
//                    in,
//                    out,
//                    err
//            );
//
//        } catch (final IOException e) {
//            throw new KribiException(KribiException.SSH_ERROR, "Unable to copy " + file.getAbsolutePath() + " to remote host", e);
//        }
//    }
//
//    Shell createShell(final Host host) throws UnknownHostException {
//        return new SSH(host.getPublicIp(), port, username, context.getKeyPair().getPrivateKey());
//    }
//
//    void waitForSSH(final Host host) throws UnknownHostException {
//        LOGGER.info("Waiting SSH port to be available on {}", host.getPublicIp());
//
//        final long delayMillis = DELAY_SECONDS * 1000;
//        final String waitMessage = "... SSH port not ready. waiting for " + DELAY_SECONDS + " seconds";
//        final Shell shell = createShell(host);
//        for (int i = 0; i < MAX_RETRIES; i++) {
//            try {
//
//                new Shell.Plain(shell).exec("echo $HOSTNAME");
//                return;
//
//            } catch (final IOException e) {
//                wait(delayMillis, waitMessage);
//            }
//        }
//
//        throw new KribiException(KribiException.SSH_ERROR, "Unable to establish SSH connection");
//    }

    void healthCheck(final String domainName, final DeployRequest deployRequest) {
        final String url = "http://" + domainName + deployRequest.getApplication().getHealthCheckPath();
        LOGGER.info("Performing healthcheck on {}", url);

        final long delayMillis = DELAY_SECONDS * 1000;
        final String waitMessage = "... Healcheck failed. waiting for " + DELAY_SECONDS + " seconds";

        for (int i = 0; i < MAX_RETRIES; i++) {
            try {

                final int code = connect(url);
                if (code == 200) {
                    LOGGER.info("{} UP", url);
                    return;
                } else {
                    wait(delayMillis, waitMessage);
                }

            } catch (final IOException e) {
                wait(delayMillis, waitMessage);
            }
        }

        throw new KribiException(KribiException.HEALCHECK_FAILED, "Healthcheck failed. The service hasn't started successfully");
    }

    int connect(final String url) throws IOException {
        final HttpURLConnection cnn = (HttpURLConnection) new URL(url).openConnection();
        try {
            cnn.setRequestMethod("GET");
            cnn.connect();
            return cnn.getResponseCode();
        } finally {
            cnn.disconnect();
        }
    }

    void wait(final long delayMillis, final String msg) {
        LOGGER.info(msg);
        try {
            Thread.sleep(delayMillis);
        } catch (final InterruptedException ex) {
        }
    }
}
