package io.tchepannou.kribi.aws.services;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.Tag;
import io.tchepannou.kribi.aws.support.AwsSupport;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.model.Services;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ELB {
    private static final Logger LOGGER = LoggerFactory.getLogger(ELB.class);
    private static final String TAG_APP_NAME = "AppName";
    private static final String TAG_ENVIRONMENT = "Environment";
    private static final String TAG_VERSION = "Version";
    private static final String TAG_TRANSACTION_ID = "TransactionId";

    private final AwsContext context;

    private final AmazonEC2 ec2;
    private final AmazonElasticLoadBalancing elb;

    public ELB(final AwsContext context) {
        elb = context.getElb();
        ec2 = context.getEc2();
        this.context = context;
    }

    public String create(final DeployRequest deployRequest, final Reservation reservation) {
        final String dnsName = createLoadBalancer(deployRequest);
        LOGGER.info("LoadBalancer created. DNS={}", dnsName);

        attachInstances(deployRequest, reservation);
        return dnsName;
    }

    public void delete(final UndeployRequest undeployRequest) {
        deleteLoadBalancer(undeployRequest);
    }

    String createLoadBalancer(final DeployRequest deployRequest) {
        final Application app = deployRequest.getApplication();
        final String name = getLoadBalancerName(deployRequest);
        final Optional<LoadBalancerDescription> loadBalancer = findLoadBalancer(deployRequest);
        if (loadBalancer.isPresent()) {
            return loadBalancer.get().getDNSName();
        }

        LOGGER.info("Creating LoadBalancer {}", name);
        final CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
                .withLoadBalancerName(name)
                .withSecurityGroups(getSecurityGroups(app))
                .withListeners(getListeners(app))
                .withTags(getTags(deployRequest))
                .withAvailabilityZones(getAvailabilityZones(deployRequest));

        final CreateLoadBalancerResult result = elb.createLoadBalancer(request);
        return result.getDNSName();
    }

    void deleteLoadBalancer(final UndeployRequest undeployRequest) {
        final String name = getLoadBalancerName(undeployRequest);
        final Optional<LoadBalancerDescription> loadBalancer = findLoadBalancer(undeployRequest);
        if (!loadBalancer.isPresent()) {
            LOGGER.info("LoadBalancer {} not found. Nothing to delete", name);
            return;
        }

        LOGGER.info("Undeploying LoadBalancer {}", name);
        final DeleteLoadBalancerRequest request = new DeleteLoadBalancerRequest()
                .withLoadBalancerName(name);
        elb.deleteLoadBalancer(request);
    }

    Optional<LoadBalancerDescription> findLoadBalancer(final KribiRequest request) {
        final String name = getLoadBalancerName(request);
        LOGGER.info("Resolving LoadBalancer {}", name);

        final DescribeLoadBalancersResult result = elb.describeLoadBalancers();
        return result.getLoadBalancerDescriptions().stream()
                .filter(lb -> lb.getLoadBalancerName().equals(name))
                .findFirst();
    }

    void attachInstances(final DeployRequest deployRequest, final Reservation reservation) {
        final String name = getLoadBalancerName(deployRequest);
        LOGGER.info("Attaching instances to LoadBalancer {}", name);

        final RegisterInstancesWithLoadBalancerRequest request = new RegisterInstancesWithLoadBalancerRequest()
                .withLoadBalancerName(name)
                .withInstances(toInstances(reservation));
        elb.registerInstancesWithLoadBalancer(request);
    }

    private Collection<Instance> toInstances(final Reservation reservation) {
        return reservation.getInstances().stream()
                .map(i -> new Instance(i.getInstanceId()))
                .collect(Collectors.toList());
    }

    private String getLoadBalancerName(final KribiRequest deployRequest) {
        final Application app = deployRequest.getApplication();
        final Environment env = deployRequest.getEnvironment();
        final String name = String.format("ELB_%s_%s_%s", app.getName(), env, AwsSupport.shortenVersion(deployRequest.getVersion()));
        return AwsSupport.nomalizeName(name);
    }

    List<String> getSecurityGroups(final Application app) {
        final Set<String> securityGroups = new HashSet<>();
        final Services services = app.getServices();
        if (services.isHttp()) {
            securityGroups.add(context.getSecurityGroups().getHttp());
        }
        if (services.isHttps()) {
            securityGroups.add(context.getSecurityGroups().getHttps());
        }
        return securityGroups.stream()
                .filter(sg -> sg != null)
                .collect(Collectors.toList());
    }

    List<Listener> getListeners(final Application app) {
        final List<Listener> listeners = new ArrayList<>();
        final Services services = app.getServices();
        if (services.isHttp()) {
            listeners.add(new Listener("http", context.getLoadBalancer().getHttpPort(), services.getHttpPort()));
        }
        if (services.isHttps()) {
            listeners.add(new Listener("https", context.getLoadBalancer().getHttpsPort(), services.getHttpsPort()));
        }
        return listeners;
    }

    List<Tag> getTags(final DeployRequest deployRequest) {
        final List<Tag> tags = new ArrayList<>();
        final Application app = deployRequest.getApplication();

        addTag(TAG_VERSION, deployRequest.getVersion(), tags);
        addTag(TAG_APP_NAME, app.getName(), tags);
        addTag(TAG_ENVIRONMENT, deployRequest.getEnvironment().name(), tags);
        addTag(TAG_TRANSACTION_ID, deployRequest.getTransactionId(), tags);
        return tags;
    }

    void addTag(final String name, final String value, final List<Tag> tags) {
        if (value == null || value.length() == 0) {
            return;
        }

        final Tag tag = new Tag();
        tag.setKey(name);
        tag.setValue(value);
        tags.add(tag);
    }

    List<String> getAvailabilityZones(final DeployRequest deployRequest) {
        final String region = deployRequest.getRegion();
        return ec2.describeAvailabilityZones().getAvailabilityZones().stream()
                .filter(az -> az.getRegionName().equals(region))
                .map(az -> az.getZoneName())
                .collect(Collectors.toList());
    }
}
