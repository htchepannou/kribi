package io.tchepannou.kribi.aws;

import com.amazonaws.services.ec2.model.Reservation;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.client.ReleaseResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.model.Host;
import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import io.tchepannou.kribi.services.Deployer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Deploy and run a JavaApplication on a EC2
 * The deployment will deploy:
 * <ul>
 * <li>Deploy 1 EC2 instance</li>
 * <li>Install the application</li>
 * <li>Start the application</li>
 * <li>Destroy the application after the application is terminated</li>
 * </ul>
 */
public class JavaAppDeployer implements Deployer {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaAppDeployer.class);

    private final EC2 ec2;

    public JavaAppDeployer(final AwsContext context) {
        this.ec2 = new EC2(context);
        scheduleVaccum();
    }

    @Override
    public DeployResponse deploy(final DeployRequest deployRequest) {
        final Cluster cluster = deployCluster(deployRequest);
        ec2.install(deployRequest, cluster);

        return new DeployResponse(cluster);
    }

    @Override
    public UndeployResponse undeploy(final UndeployRequest request) {
        ec2.delete(request);

        return new UndeployResponse();
    }

    @Override
    public ReleaseResponse release(final ReleaseRequest request) {
        return new ReleaseResponse();
    }

    @Override
    public Collection<String> getVersions(final String name, final Environment env) {
        return ec2.getVersions(name, env);
    }

    private void vacuum() {
        final Collection<Reservation> reservations = ec2.getReservationsByTemplate(ApplicationTemplate.javaapp);
        final long now = System.currentTimeMillis();
        final long oneHour = 3600 * 1000;
        for (final Reservation reservation : reservations) {
            if (reservation.getInstances().get(0).getLaunchTime().getTime() - now > oneHour) {
                try {
                    ec2.delete(reservation);
                } catch (final Exception e) {
                    LOGGER.error("Unable to delete Reservation: {}", reservation.getReservationId(), e);
                }
            }
        }
    }

    private Cluster deployCluster(final DeployRequest request) {
        final Reservation reservation = ec2.create(request);
        final Host host = toHost(reservation.getInstances().get(0));

        final Cluster cluster = new Cluster();
        cluster.setTransactionId(request.getTransactionId());
        cluster.setHosts(Collections.singletonList(host));
        return cluster;
    }

    private Host toHost(final com.amazonaws.services.ec2.model.Instance instance) {
        final Host host = new Host();
        host.setId(instance.getInstanceId());
        host.setPrivateIp(instance.getPrivateIpAddress());
        host.setPublicIp(instance.getPublicIpAddress());
        return host;
    }

    private void scheduleVaccum() {
        final Runnable task = new Runnable() {
            @Override
            public void run() {
                LOGGER.info("Running vaccum...");
                try {
                    vacuum();
                } catch (final Exception e) {
                    LOGGER.error("Unexpected error", e);
                }
            }
        };

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(task, 0, 1, TimeUnit.HOURS);
    }
}
