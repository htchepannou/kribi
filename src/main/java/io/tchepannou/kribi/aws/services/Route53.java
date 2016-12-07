package io.tchepannou.kribi.aws.services;

import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Optional;

public class Route53 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Route53.class);

    private final AwsContext context;
    private final AmazonRoute53 route53;
    private final ELB elb;

    public Route53(final AwsContext context) {
        this.context = context;
        this.route53 = context.getRoute53();
        this.elb = new ELB(context);
    }

    public String release(final ReleaseRequest request) {
        final Optional<HostedZone> hostedZone = findHostedZone();
        if (!hostedZone.isPresent()) {
            throw new KribiException(KribiException.HOSTED_ZONE_NOT_FOUND, "No HostedZone found with id=" + context.getHostedZoneId());
        }

        final Optional<LoadBalancerDescription> lb = elb.findLoadBalancer(request);
        if (!lb.isPresent()){
            throw new KribiException(KribiException.LOAD_BALANCER_NOT_FOUND, "Load balancer not found for " + request.getApplicationName() + "." + request.getVersion());
        }

        final RRType type = RRType.CNAME;
        final ResourceRecord rr = new ResourceRecord(lb.get().getDNSName());

        final ResourceRecordSet resourceRecordSet = new ResourceRecordSet();
        final String domainName = getARecordName(request, hostedZone.get());
        resourceRecordSet.setName(domainName);
        resourceRecordSet.setType(type);
        resourceRecordSet.setTTL(new Long(300));
        resourceRecordSet.setResourceRecords(Collections.singletonList(rr));

        final Change change = new Change(ChangeAction.UPSERT, resourceRecordSet);
        final ChangeBatch changeBatch = new ChangeBatch(Collections.singletonList(change));

        LOGGER.info("Adding {} {} -> {}", type, domainName, rr.getValue());
        final ChangeResourceRecordSetsRequest req = new ChangeResourceRecordSetsRequest(hostedZone.get().getId(), changeBatch);
        route53.changeResourceRecordSets(req);

        return domainName.substring(0, domainName.length()-1);
    }

    Optional<HostedZone> findHostedZone() {
        final String id = context.getHostedZoneId();
        LOGGER.info("Resolving HostedZone {}", id);

        return route53.listHostedZones().getHostedZones().stream()
                .filter(z -> z.getId().equals(id))
                .findFirst();
    }

    private String getARecordName(final KribiRequest deployRequest, final HostedZone hostedZone) {
        final Application app = deployRequest.getApplication();

        return String.format("%s.%s.%s", app.getName(), deployRequest.getRegion(), hostedZone.getName());
    }
}
