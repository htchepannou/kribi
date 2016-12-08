package io.tchepannou.kribi.client;

import java.util.ArrayList;
import java.util.List;

public class ReleaseResponse extends KribiResponse {
    private List<UndeployResponse> undeployResponses = new ArrayList<>();
    private List<ErrorResponse> undeployErrors = new ArrayList<>();

    public void addUndeployResponse(UndeployResponse response){
        undeployResponses.add(response);
    }

    public void addUndeployError(ErrorResponse errorResponse){
        undeployErrors.add(errorResponse);
    }

    public List<UndeployResponse> getUndeployResponses() {
        return undeployResponses;
    }

    public void setUndeployResponses(final List<UndeployResponse> undeployResponses) {
        this.undeployResponses = undeployResponses;
    }

    public List<ErrorResponse> getUndeployErrors() {
        return undeployErrors;
    }

    public void setUndeployErrors(final List<ErrorResponse> undeployErrors) {
        this.undeployErrors = undeployErrors;
    }
}
