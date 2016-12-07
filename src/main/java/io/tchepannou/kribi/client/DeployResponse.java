package io.tchepannou.kribi.client;

import io.tchepannou.kribi.model.Cluster;
import io.swagger.annotations.ApiModel;

@ApiModel
public class DeployResponse extends KribiResponse {
    private Cluster cluster;
    private ReleaseResponse releaseResponse;

    public DeployResponse (final Cluster cluster) {
        this.cluster = cluster;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(final Cluster cluster) {
        this.cluster = cluster;
    }

    public ReleaseResponse getReleaseResponse() {
        return releaseResponse;
    }

    public void setReleaseResponse(final ReleaseResponse releaseResponse) {
        this.releaseResponse = releaseResponse;
    }
}
