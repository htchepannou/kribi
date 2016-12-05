package io.tchepannou.kribi.services;

import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.model.Cluster;

public interface Installer {
    int DELAY_SECONDS = 60;
    int MAX_RETRIES = 10;

    void install(final DeployRequest deployRequest, final Cluster cluster);
}
