package io.tchepannou.kribi.client;

import io.tchepannou.kribi.model.Application;

public class ArtifactResponse extends KribiResponse{
    public Application application;

    public Application getApplication() {
        return application;
    }

    public void setApplication(final Application application) {
        this.application = application;
    }
}
