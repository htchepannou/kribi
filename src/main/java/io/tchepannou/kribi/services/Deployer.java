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
    /**
     * Deploy a new application
     */
    DeployResponse deploy(DeployRequest request);

    /**
     * Undeploy an application
     */
    UndeployResponse undeploy(UndeployRequest request);

    /**
     * Release an application
     */
    ReleaseResponse release(ReleaseRequest request);

    /**
     * Return all the versions of a deployed application
     *
     * @param name - application name
     * @param env - application evironment
     * @return List of version
     */
    Collection<String> getVersions(final String name, final Environment env);
}
