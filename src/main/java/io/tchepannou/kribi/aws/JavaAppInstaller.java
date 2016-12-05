package io.tchepannou.kribi.aws;

import io.tchepannou.kribi.aws.services.EC2;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.services.Installer;

public class JavaAppInstaller implements Installer{
    private final EC2 ec2;

    public JavaAppInstaller(final AwsContext context) {
        this.ec2 = new EC2(context);
    }

    public void install(final DeployRequest deployRequest, final Cluster cluster) {
        ec2.install(deployRequest, cluster);
    }
}
