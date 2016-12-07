package io.tchepannou.kribi.client;

public class ReleaseRequest extends KribiRequest{
    private boolean delete;

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(final boolean delete) {
        this.delete = delete;
    }
}
