package io.tchepannou.kribi.client;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.tchepannou.kribi.model.Environment;

@ApiModel
public class KribiResponse {
    private String transactionId;
    private String applicationName;
    private String version;
    private String region;
    private Environment environment;

    public KribiResponse(){

    }

    @ApiModelProperty(value = "Unique identifier of the transaction")
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(final String region) {
        this.region = region;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
}
