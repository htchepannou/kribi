package io.tchepannou.kribi;

public class KribiException extends RuntimeException {
    public static final String DEPLOYMENT_INTERRUPTED = "DEPLOYMENT_INTERRUPTED";
    public static final String DEPLOYMENT_TIMEOUT = "DEPLOYMENT_TIMEOUT";
    public static final String APPLICATION_ALREADY_DEPLOYED = "APPLICATION_ALREADY_DEPLOYED";
    public static final String SSH_ERROR = "SSH_ERROR";
    public static final String INSTALL_SCRIPTS_NOT_FOUND = "INSTALL_SCRIPTS_NOT_FOUND";
    public static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";
    public static final String INSTALLER_NOT_AVAILABLE = "INSTALLER_NOT_AVAILABLE";
    public static final String DEPLOYER_NOT_AVAILABLE = "DEPLOYER_NOT_AVAILABLE";
    public static final String AWS_AMI_NOT_AVAILABLE = "AWS_AMI_NOT_AVAILABLE";
    public static final String HEALCHECK_FAILED = "HEALCHECK_FAILED";
    public static final String HOSTED_ZONE_NOT_FOUND = "HOSTED_ZONE_NOT_FOUND";
    public static final String ROLE_NOT_FOUND = "ROLE_NOT_FOUND";
    public static final String ARTIFACT_MALFORMED = "ARTIFACT_MALFORMED";
    public static final String APPLICATION_DESCRIPTOR_MALFORMED = "APPLICATION_DESCRIPTOR_MALFORMED";
    public static final String APPLICATION_DESCRIPTOR_NOT_FOUND = "APPLICATION_DESCRIPTOR_NOT_FOUND";

    private final String code;
    private final String text;

    public KribiException(final String code, final String text) {
        this.code = code;
        this.text = text;
    }
    public KribiException(final String code, final String text, Throwable cause) {
        super(cause);

        this.code = code;
        this.text = text;
    }

    @Override
    public String getMessage() {
        return code + " - " + text;
    }

    public String getCode() {
        return code;
    }

    public String getText() {
        return text;
    }
}
