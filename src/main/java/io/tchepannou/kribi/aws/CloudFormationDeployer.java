package io.tchepannou.kribi.aws;

import com.amazonaws.services.ec2.model.Reservation;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.aws.services.ELB;
import io.tchepannou.kribi.aws.services.Route53;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.client.ReleaseResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.model.Host;
import io.tchepannou.kribi.services.Deployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Deploy a cloud formation.
 * The deployment will deploy:
 * <ul>
 * <li>Deploy EC2 instances</li>
 * <li>Deploy and ELB</li>
 * <li>Install the application</li>
 * <li>Start the application</li>
 * <li>Setup the application domain name</li>
 * </ul>
 */
public class CloudFormationDeployer implements Deployer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFormationDeployer.class);
    private static final int DELAY_SECONDS = 60;
    private static final int MAX_RETRIES = 10;

    private final EC2 ec2;
    private final ELB elb;
    private final Route53 route53;

    public CloudFormationDeployer(final AwsContext context) {
        this.ec2 = new EC2(context);
        this.elb = new ELB(context);
        this.route53 = new Route53(context);
    }

    public DeployResponse deploy(final DeployRequest deployRequest) {
        final Cluster cluster = createCluster(deployRequest);
        ec2.install(deployRequest, cluster);
        healthCheck(cluster.getDnsName(), deployRequest);

        return new DeployResponse(cluster);
    }

    public UndeployResponse undeploy(final UndeployRequest request) {
        ec2.delete(request);
        elb.delete(request);

        return new UndeployResponse();
    }

    public ReleaseResponse release(ReleaseRequest request){
        route53.release(request);

        return new ReleaseResponse();
    }

    public Collection<String> getVersions(final String name, final Environment env){
        return ec2.getVersions(name, env);
    }

    //-- Private
    private Cluster createCluster(final DeployRequest request) {
        final Reservation reservation = ec2.create(request);
        final String dnsName = elb.create(request, reservation);
        final Cluster cluster = new Cluster();
        cluster.setTransactionId(request.getTransactionId());
        cluster.setDnsName(dnsName);
        cluster.setHosts(
                reservation.getInstances().stream()
                        .map(i -> toHost(i))
                        .collect(Collectors.toList())
        );
        return cluster;
    }

    private Host toHost(final com.amazonaws.services.ec2.model.Instance instance) {
        final Host host = new Host();
        host.setId(instance.getInstanceId());
        host.setPrivateIp(instance.getPrivateIpAddress());
        host.setPublicIp(instance.getPublicIpAddress());
        return host;
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
