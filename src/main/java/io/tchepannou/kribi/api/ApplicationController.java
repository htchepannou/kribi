package io.tchepannou.kribi.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.client.ArtifactResponse;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ErrorResponse;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.ApplicationDescriptorService;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

@RestController
@RequestMapping("/v1/application")
@Api(value = "Application Management API")
public class ApplicationController {
    //-- Attributes
    @Autowired
    AWSCredentialsProvider awsCredentialsProvider;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AwsContextFactory awsContextFactory;

    @Autowired
    TransactionIdGenerator transactionIdGenerator;

    @Autowired
    ApplicationDescriptorService applicationDescriptorService;

    //-- Endpoints
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ApiOperation(value = "Deploy the application")
    public DeployResponse deploy(
            @RequestBody @Valid final DeployRequest request
    ) throws IOException {

        ensureArtifactExists(request);
        init(request);

        final AwsContext context = createAwsContext(request);
        final DeployResponse response = context.getDeployer(request.getApplication()).deploy(request);
        response.setTransactionId(request.getTransactionId());
        return response;

    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    @ApiOperation(value = "Undeploy the application")
    public UndeployResponse undeploy(
            @RequestBody @Valid final UndeployRequest request
    ) throws IOException {

        ensureArtifactExists(request);
        init(request);

        final AwsContext context = createAwsContext(request);
        final UndeployResponse response = context.getDeployer(request.getApplication()).undeploy(request);
        response.setTransactionId(request.getTransactionId());
        return response;

    }

    @RequestMapping(value = "/{name}/init_artifact", method = RequestMethod.POST)
    @ApiOperation(value = "Initialize the application artifact")
    public ArtifactResponse initArtifact(
            @PathVariable @ApiParam(value = "Name of the application") final String name,
            @RequestParam(required = true) @ApiParam(value = "Version of the application") final String version
    ) throws IOException {

        final Application app = applicationDescriptorService.extract(name, version);
        applicationDescriptorService.store(app);

        final ArtifactResponse response = new ArtifactResponse();
        response.setApplication(app);
        response.setTransactionId(transactionIdGenerator.get());
        return response;

    }

    @ExceptionHandler(KribiException.class)
    public ResponseEntity<ErrorResponse> onKribiException(final KribiException ex) {
        final ErrorResponse resp = new ErrorResponse(ex.getCode(), ex.getMessage());
        resp.setTransactionId(transactionIdGenerator.get());

        final String code = ex.getCode();
        if (KribiException.ARTIFACT_NOT_FOUND.equals(code)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
        }
    }

    //-- Private
    private void init(final KribiRequest request) throws IOException {
        final Application app = applicationDescriptorService.load(request.getApplicationName());

        request.setTransactionId(transactionIdGenerator.get());
        request.setApplication(app);
    }

    private void ensureArtifactExists(final KribiRequest request) {
        final String name = request.getApplicationName();
        final String version = request.getVersion();
        if (!applicationDescriptorService.isValid(name, version)) {
            throw new KribiException(KribiException.ARTIFACT_NOT_FOUND, "Application not found " + name + "." + version);
        }
    }

    private AwsContext createAwsContext(final KribiRequest request) {
        final Account account = accountRepository.findAccount();
        return awsContextFactory.create(account, request, awsCredentialsProvider);
    }
}
