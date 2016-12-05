package io.tchepannou.kribi.services;

import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;

public interface Deployer {
    DeployResponse deploy(DeployRequest request);

    UndeployResponse undeploy(UndeployRequest request);
}
