package io.tchepannou.kribi.model;

public class Services {
    private boolean http;
    private boolean https;
    private boolean ssh;
    private int httpPort = 8080;
    private int httpsPort = 8443;
    private int sshPort = 22;

    public boolean isHttp() {
        return http;
    }

    public void setHttp(final boolean http) {
        this.http = http;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(final boolean https) {
        this.https = https;
    }

    public boolean isSsh() {
        return ssh;
    }

    public void setSsh(final boolean ssh) {
        this.ssh = ssh;
    }

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

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(final int sshPort) {
        this.sshPort = sshPort;
    }
}
