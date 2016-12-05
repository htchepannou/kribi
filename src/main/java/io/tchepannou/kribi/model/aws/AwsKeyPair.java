package io.tchepannou.kribi.model.aws;

public class AwsKeyPair {
    private String name;
    private String privateKey;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(final String privateKey) {
        this.privateKey = privateKey;
    }
}
