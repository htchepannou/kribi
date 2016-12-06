package io.tchepannou.kribi.client;

public class ErrorResponse extends KribiResponse{
    private String code;
    private String message;

    public ErrorResponse(final String code, final String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }
}

