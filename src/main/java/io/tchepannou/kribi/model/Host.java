package io.tchepannou.kribi.model;

public class Host {
    private String id;
    private String privateIp;
    private String publicIp;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(final String privateIp) {
        this.privateIp = privateIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(final String publicIp) {
        this.publicIp = publicIp;
    }
}
