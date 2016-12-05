package io.tchepannou.kribi.client;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KribiResponse {
    private String transactionId;

    public KribiResponse(){

    }

    @ApiModelProperty(value = "Unique identifier of the transaction")
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(final String transactionId) {
        this.transactionId = transactionId;
    }
}
