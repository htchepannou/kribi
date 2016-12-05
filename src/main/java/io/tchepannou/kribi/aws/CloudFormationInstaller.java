package io.tchepannou.kribi.aws;

import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.aws.services.Route53;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.services.Installer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CloudFormationInstaller implements Installer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationInstaller.class);

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
        healthCheck(cluster.getDnsName(), deployRequest);

        final String domainName = route53.deploy(deployRequest, cluster);
        healthCheck(domainName, deployRequest);
    }

    public void release(final ReleaseRequest releaseRequest){

    }

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
