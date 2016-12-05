package io.tchepannou.kribi.client;

public class ReleaseRequest extends DeployRequest{
    private boolean delete;

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(final boolean delete) {
        this.delete = delete;
    }
}
