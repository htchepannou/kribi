package io.tchepannou.kribi.model.aws;

public class AwsSecurityGroups {
    private String http;
    private String https;
    private String ssh;

    public String getHttp() {
        return http;
    }

    public void setHttp(final String http) {
        this.http = http;
    }

    public String getHttps() {
        return https;
    }

    public void setHttps(final String https) {
        this.https = https;
    }

    public String getSsh() {
        return ssh;
    }

    public void setSsh(final String ssh) {
        this.ssh = ssh;
    }
}
