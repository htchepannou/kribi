package io.tchepannou.kribi.model;

import io.tchepannou.kribi.model.aws.AwsKeyPair;
import io.tchepannou.kribi.model.aws.AwsLoadBalancer;
import io.tchepannou.kribi.model.aws.AwsSecurityGroups;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.UUID;

public class Account {
    private String name;
    private String apiKey;
    private String hostedZoneId;
    private AwsKeyPair keyPair = new AwsKeyPair();
    private AwsSecurityGroups securityGroups = new AwsSecurityGroups();
    private AwsLoadBalancer loadBalancer = new AwsLoadBalancer();

    public static KeyPair generateApiKeyPair(final Account account) {
        final String publicKey = UUID.randomUUID().toString();
        final String privateKey = generatePrivateKey(publicKey, account);

        return new KeyPair(publicKey, privateKey);
    }

    private static String generatePrivateKey(final String publicKey, final Account account){
        final String salt = account.getName() + "." + account.getKeyPair().getPrivateKey();
        return DigestUtils.md5Hex(publicKey + "-" + salt);
    }

    public boolean apiKeyMatches(final String apiKey) {
        final String privateKey = generatePrivateKey(apiKey, this);
        return privateKey.equals(this.apiKey);
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getHostedZoneId() {
        return hostedZoneId;
    }

    public void setHostedZoneId(final String hostedZoneId) {
        this.hostedZoneId = hostedZoneId;
    }

    public AwsKeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(final AwsKeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public AwsSecurityGroups getSecurityGroups() {
        return securityGroups;
    }

    public void setSecurityGroups(final AwsSecurityGroups securityGroups) {
        this.securityGroups = securityGroups;
    }

    public AwsLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public void setLoadBalancer(final AwsLoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }
}
