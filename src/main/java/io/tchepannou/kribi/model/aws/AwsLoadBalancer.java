package io.tchepannou.kribi.model.aws;

public class AwsLoadBalancer {
    private int httpPort = 80;
    private int httpsPort = 443;

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(final int httpPort) {
        this.httpPort = httpPort;
    }

    public int getHttpsPort() {
        return httpsPort;
    }

    public void setHttpsPort(final int httpsPort) {
        this.httpsPort = httpsPort;
    }
}
