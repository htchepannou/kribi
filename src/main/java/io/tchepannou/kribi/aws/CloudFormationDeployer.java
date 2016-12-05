package io.tchepannou.kribi.aws;

import com.amazonaws.services.ec2.model.Reservation;
import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.aws.services.ELB;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Host;
import io.tchepannou.kribi.services.Deployer;

import java.util.stream.Collectors;

/**
 * Deploy a cloud formation.
 * The deployment will deploy:
 * <ul>
 *     <li>Deploy EC2 instances</li>
 *     <li>Deploy and ELB</li>
 *     <li>Install the application</li>
 *     <li>Start the application</li>
 *     <li>Setup the application domain name</li>
 * </ul>
 */
public class CloudFormationDeployer implements Deployer {
    private final EC2 ec2;
    private final ELB elb;
    private final AwsContext context;

    public CloudFormationDeployer(final AwsContext context) {
        this.ec2 = new EC2(context);
        this.elb = new ELB(context);
        this.context = context;
    }

    public DeployResponse deploy(final DeployRequest request) {
        final Cluster cluster = createCluster(request);
        context.getInstaller(request.getApplication()).install(request, cluster);

        return new DeployResponse(cluster);
    }

    public UndeployResponse undeploy(final UndeployRequest request) {
        ec2.delete(request);
        elb.delete(request);

        return new UndeployResponse();
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
}
