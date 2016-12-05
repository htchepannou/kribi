package io.tchepannou.kribi.aws.services;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.Role;
import io.tchepannou.kribi.model.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class IAM {
    private static final Logger LOGGER = LoggerFactory.getLogger(IAM.class);

    private final AmazonIdentityManagement iam;

    private IAM(final AWSCredentialsProvider credentialsProvider) {
        this.iam = AmazonIdentityManagementClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withRegion(Regions.DEFAULT_REGION)
                .build();
    }

    public static IAM create(final AWSCredentialsProvider credentialsProvider) {
        return new IAM(credentialsProvider);
    }

    public Role createRole(final Application app) {
        final String roleName = getRoleName(app);
        LOGGER.info("Application role: {}", roleName);

        final Optional<Role> role = findRole(roleName);
        if (role.isPresent()) {
            return role.get();
        }

        LOGGER.info("Creating Role {}", roleName);
        final CreateRoleRequest request = createRoleRequest(app);
        final CreateRoleResult result = iam.createRole(request);
        LOGGER.info("Role {} has been created", result.getRole().getArn());

        return result.getRole();
    }

    private CreateRoleRequest createRoleRequest(final Application app) {
        final String roleName = getRoleName(app);
        final CreateRoleRequest request = new CreateRoleRequest()
                .withRoleName(roleName)
                .withAssumeRolePolicyDocument("{\n"
                        + "  \"Version\": \"2012-10-17\",\n"
                        + "  \"Statement\": {\n"
                        + "    \"Effect\": \"Allow\",\n"
                        + "    \"Principal\": {\"Service\": \"ec2.amazonaws.com\"},\n"
                        + "    \"Action\": \"sts:AssumeRole\"\n"
                        + "  }\n"
                        + "}");
        return request;
    }

    private String getRoleName(final Application app) {
        return app.getName();
    }

    private Optional<Role> findRole(final String name) {
        LOGGER.info("Resolving Role: ", name);

        final ListRolesResult result = iam.listRoles();
        return result.getRoles().stream()
                .filter(r -> r.getRoleName().equals(name))
                .findFirst();
    }
}
