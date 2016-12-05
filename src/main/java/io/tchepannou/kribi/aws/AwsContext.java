package io.tchepannou.kribi.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementAsyncClientBuilder;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53AsyncClientBuilder;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.OS;
import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import io.tchepannou.kribi.model.aws.AwsAMI;
import io.tchepannou.kribi.model.aws.AwsKeyPair;
import io.tchepannou.kribi.model.aws.AwsLoadBalancer;
import io.tchepannou.kribi.model.aws.AwsSecurityGroups;
import io.tchepannou.kribi.services.Deployer;
import io.tchepannou.kribi.services.Installer;

public class AwsContext {
    private String hostedZoneId;
    private AwsSecurityGroups securityGroups = new AwsSecurityGroups();
    private AwsLoadBalancer loadBalancer = new AwsLoadBalancer();
    private AwsKeyPair keyPair = new AwsKeyPair();
    private final AmazonEC2 ec2;
    private final AmazonElasticLoadBalancing elb;
    private final AmazonRoute53 route53;
    private final AmazonIdentityManagement iam;

    public AwsContext(final AWSCredentialsProvider credentialsProvider, final String region) {
        elb = AmazonElasticLoadBalancingClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();

        ec2 = AmazonEC2ClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();

        route53 = AmazonRoute53AsyncClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();

        iam = AmazonIdentityManagementAsyncClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(region)
                .build();
    }

    public Installer getInstaller(final Application application) {
        final ApplicationTemplate template = application.getTemplate();
        if (ApplicationTemplate.springboot.equals(template)) {
            return new CloudFormationInstaller(this);
        } else if (ApplicationTemplate.javaapp.equals(template)){
            return new JavaAppInstaller(this);
        } else {
            throw new KribiException(KribiException.INSTALLER_NOT_AVAILABLE, "No installer available for application template: " + template);
        }
    }

    public Deployer getDeployer(final Application application) {
        final ApplicationTemplate template = application.getTemplate();
        if (ApplicationTemplate.springboot.equals(template)) {
            return new CloudFormationDeployer(this);
        } else if (ApplicationTemplate.javaapp.equals(template)){
            return new JavaAppDeployer(this);
        } else {
            throw new KribiException(KribiException.DEPLOYER_NOT_AVAILABLE, "No deployer available for application template: " + template);
        }
    }

    public AwsLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public AwsSecurityGroups getSecurityGroups() {
        return securityGroups;
    }

    public AwsAMI getAMI(final Application application) {
        final OS os = application.getOperatingSystem();
        if (OS.LINUX.equals(os)) {
            return AwsAMI.AMAZON_LINUX;
        }

        throw new KribiException(KribiException.AWS_AMI_NOT_AVAILABLE, "No API available for OS: " + os);
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(final String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public void setSecurityGroups(final AwsSecurityGroups securityGroups) {
        this.securityGroups = securityGroups;
    }

    public void setLoadBalancer(final AwsLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public AwsKeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(final AwsKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public AmazonEC2 getEc2() {
        return ec2;
    }

    public AmazonIdentityManagement getIam() {
        return iam;
    }

    public AmazonElasticLoadBalancing getElb() {
        return elb;
    }

    public AmazonRoute53 getRoute53() {
        return route53;
    }
}
