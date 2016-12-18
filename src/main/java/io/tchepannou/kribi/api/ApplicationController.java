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
import io.tchepannou.kribi.client.KribiResponse;
import io.tchepannou.kribi.client.ReleaseRequest;
import io.tchepannou.kribi.client.ReleaseResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.ApplicationDescriptorService;
import io.tchepannou.kribi.services.Deployer;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;

@RestController
@RequestMapping("/v1/application")
@Api(value = "Application Management API")
public class ApplicationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

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
    @RequestMapping(value = "/{name}/deploy", method = RequestMethod.GET)
    @ApiOperation(value = "Deploy the application")
    public DeployResponse deploy(
            @PathVariable @ApiParam(value = "Name of the application") final String name,
            @RequestParam(required = true) @ApiParam(value = "Version of the application") final String version,
            @RequestParam(required = true) @ApiParam(value = "Environment where to deploy", defaultValue = "TEST", allowableValues = "TEST,INT,PROD")
            final String environment,
            @RequestParam(required = true) @ApiParam(value = "Region where to deploy", defaultValue = "us-east-1") final String region,
            @RequestParam(required = false) @ApiParam(value = "Automatically release the new version", defaultValue = "true") final boolean release,
            @RequestParam(required = false) @ApiParam(value = "If release, undeploy previous version", defaultValue = "true") final boolean undeployOld
    ) throws IOException {
        LOGGER.info("Deploying {}.{} on {} in environment {}", name, version, region, environment);

        ensureArtifactExists(name, version);

        final DeployRequest request = new DeployRequest();
        init(name, version, environment, region, request);

        final AwsContext context = createAwsContext(request);

        /* Deploy */
        final DeployResponse response = context.getDeployer(request.getApplication()).deploy(request);

        /* Release */
        if (release) {
            final ReleaseResponse releaseResponse = release(name, version, environment, region, undeployOld);
            response.setReleaseResponse(releaseResponse);
        }

        initResponse(request, response);
        LOGGER.info("SUCCESS");
        return response;
    }

    @RequestMapping(value = "/{name}/undeploy", method = RequestMethod.GET)
    @ApiOperation(value = "Undeploy the application")
    public UndeployResponse undeploy(
            @PathVariable @ApiParam(value = "Name of the application") final String name,
            @RequestParam(required = true) @ApiParam(value = "Version of the application") final String version,
            @RequestParam(required = true) @ApiParam(value = "Environment where to undeploy", defaultValue = "TEST", allowableValues = "TEST,INT,PROD")
            final String environment,
            @RequestParam(required = true) @ApiParam(value = "Region where to undeploy", defaultValue = "us-east-1") final String region
    ) throws IOException {
        LOGGER.info("Undeploying {}.{} on {} in environment {}", name, version, region, environment);

        final UndeployRequest request = new UndeployRequest();
        init(name, version, environment, region, request);

        final AwsContext context = createAwsContext(request);
        final UndeployResponse response = context.getDeployer(request.getApplication()).undeploy(request);
        initResponse(request, response);
        return response;

    }

    @RequestMapping(value = "/{name}/release", method = RequestMethod.GET)
    @ApiOperation(value = "Release the application")
    public ReleaseResponse release(
            @PathVariable @ApiParam(value = "Name of the application") final String name,
            @RequestParam(required = true) @ApiParam(value = "Version of the application") final String version,
            @RequestParam(required = true) @ApiParam(value = "Environment where to release", defaultValue = "TEST", allowableValues = "TEST,INT,PROD")
            final String environment,
            @RequestParam(required = true) @ApiParam(value = "Region where to release", defaultValue = "us-east-1") final String region,
            @RequestParam(required = false) @ApiParam(value = "If release, undeploy previous version", defaultValue = "true") final boolean undeployOld
    ) throws IOException {
        LOGGER.info("Releasing {}.{} on {} in environment {}", name, version, region, environment);

        ensureArtifactExists(name, version);

        final ReleaseRequest request = new ReleaseRequest();
        init(name, version, environment, region, request);

        final AwsContext context = createAwsContext(request);

        /* Release */
        final Deployer deployer = context.getDeployer(request.getApplication());
        final ReleaseResponse response = deployer.release(request);
        initResponse(request, response);

        /* Delete previous version */
        if (undeployOld) {
            final Collection<String> versions = deployer.getVersions(name, Environment.valueOf(environment.toUpperCase()));
            LOGGER.info("{} version(s) to undeploy", versions.size() - 1);

            for (final String ver : versions) {
                if (!ver.equals(version)) {
                    try {
                        final UndeployResponse undeployResponse = undeploy(name, ver, environment, region);
                        response.getUndeployResponses().add(undeployResponse);
                    } catch (final KribiException e) {
                        final ErrorResponse errorResponse = new ErrorResponse(e.getCode(), e.getMessage());
                        initResponse(request, errorResponse);
                        response.addUndeployError(errorResponse);
                    }
                }
            }
        }

        return response;

    }

    @RequestMapping(value = "/{name}/init_artifact", method = RequestMethod.GET)
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
    private void init(final String name, final String version, final String environment, final String region, final KribiRequest request)
            throws IOException {
        request.setApplicationName(name);
        request.setVersion(version);
        request.setEnvironment(Environment.valueOf(environment.toUpperCase()));
        request.setRegion(region);

        final Application app = applicationDescriptorService.load(request.getApplicationName());

        request.setTransactionId(transactionIdGenerator.get());
        request.setApplication(app);
    }

    private void initResponse(final KribiRequest request, final KribiResponse response) {
        response.setApplicationName(request.getApplicationName());
        response.setEnvironment(request.getEnvironment());
        response.setRegion(request.getRegion());
        response.setTransactionId(transactionIdGenerator.get());
        response.setVersion(request.getVersion());
    }

    private void ensureArtifactExists(final String name, final String version) {
        if (!applicationDescriptorService.isValid(name, version)) {
            throw new KribiException(KribiException.ARTIFACT_NOT_FOUND, "Application not found " + name + "." + version);
        }
    }

    private AwsContext createAwsContext(final KribiRequest request) {
        final Account account = accountRepository.findAccount();
        return awsContextFactory.create(account, request, awsCredentialsProvider);
    }
}
