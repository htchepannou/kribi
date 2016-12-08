package io.tchepannou.kribi.services;

import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.client.ReleaseResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Environment;

import java.util.Collection;

public interface Deployer {
    DeployResponse deploy(DeployRequest request);

    UndeployResponse undeploy(UndeployRequest request);

    ReleaseResponse release(ReleaseRequest request);

    Collection<String> getVersions(final String name, final Environment env);
}
