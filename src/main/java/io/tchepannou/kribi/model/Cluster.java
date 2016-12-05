package io.tchepannou.kribi.model;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
    private String transactionId;
    private String dnsName;
    private List<Host> hosts = new ArrayList<>();

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public String getDnsName() {
        return dnsName;
    }

    public void setDnsName(final String dnsName) {
        this.dnsName = dnsName;
    }

    public List<Host> getHosts() {
        return hosts;
    }

    public void setHosts(final List<Host> hosts) {
        this.hosts = hosts;
    }
}
