package io.tchepannou.kribi.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.model.Account;

public class AwsContextFactory {
    public AwsContext create(
            final Account account,
            final KribiRequest request,
            final AWSCredentialsProvider awsCredentialsProvider
    ) {
        final AwsContext ctx = new AwsContext(awsCredentialsProvider, request.getRegion());
        ctx.setHostedZoneId(account.getHostedZoneId());
        ctx.setKeyPair(account.getKeyPair());
        ctx.setLoadBalancer(account.getLoadBalancer());
        ctx.setSecurityGroups(account.getSecurityGroups());

        return ctx;
    }
}
