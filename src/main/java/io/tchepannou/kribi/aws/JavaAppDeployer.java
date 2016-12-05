package io.tchepannou.kribi.aws;

import com.amazonaws.services.ec2.model.Reservation;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Host;
import io.tchepannou.kribi.services.Deployer;

import java.util.Collections;

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
public class JavaAppDeployer implements Deployer{
    private final EC2 ec2;
    private final AwsContext context;

    public JavaAppDeployer(final AwsContext context) {
        this.ec2 = new EC2(context);
        this.context = context;
    }

    @Override
    public DeployResponse deploy(final DeployRequest request) {
        final Cluster cluster = deployCluster(request);
        context.getInstaller(request.getApplication()).install(request, cluster);

        return new DeployResponse(cluster);
    }

    @Override
    public UndeployResponse undeploy(final UndeployRequest request) {
        ec2.delete(request);

        return new UndeployResponse();
    }

    private Cluster deployCluster (final DeployRequest request){
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
}
