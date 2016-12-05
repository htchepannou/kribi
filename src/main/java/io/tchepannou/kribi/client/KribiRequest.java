package io.tchepannou.kribi.client;

import io.swagger.annotations.ApiModelProperty;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.Environment;

import javax.validation.constraints.NotNull;

public abstract class KribiRequest {
    private String applicationName;
    private String version;
    private String region;
    private Environment environment;
    private Application application;
    private String transactionId;

    @ApiModelProperty(value = "Application environment", required = true)
    @NotNull
    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }

    @ApiModelProperty(value = "Geographical area of the application. Ex: us-west-1, us-east-1 etc.", required = true)
    @NotNull
    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    @ApiModelProperty(value = "Version of the application", required = true)
    @NotNull
    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @ApiModelProperty(value = "Name of the application", required = true)
    @NotNull
    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    @ApiModelProperty(hidden = true)
    public Application getApplication() {
        return application;
    }

    public void setApplication(final Application application) {
        this.application = application;
    }

    @ApiModelProperty(hidden = true)
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

}
