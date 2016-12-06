package io.tchepannou.kribi.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ErrorResponse;
import io.tchepannou.kribi.client.KribiRequest;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.client.UploadArtifactResponse;
import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.StorageService;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/v1/application")
@Api(value = "Application Management API")
public class ApplicationController {
    //-- Attributes
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationController.class);

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    AWSCredentialsProvider awsCredentialsProvider;

    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AwsContextFactory awsContextFactory;

    @Autowired
    StorageService storageService;

    @Autowired
    TransactionIdGenerator transactionIdGenerator;

    //-- Endpoints
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ApiOperation(value = "Deploy the application")
    public DeployResponse deploy(
            @RequestBody @Valid final DeployRequest request
    ) throws IOException {
        final Account account = accountRepository.findAccount();
        final AwsContext ctx = awsContextFactory.create(account, request, awsCredentialsProvider);

        init(request);

        final DeployResponse response = ctx.getDeployer(request.getApplication()).deploy(request);
        response.setTransactionId(request.getTransactionId());
        return response;
    }

    @RequestMapping(value = "/", method = RequestMethod.DELETE)
    @ApiOperation(value = "Undeploy the application")
    public UndeployResponse undeploy(
            @RequestBody @Valid final UndeployRequest request
    ) throws IOException {
        final Account account = accountRepository.findAccount();
        final AwsContext ctx = awsContextFactory.create(account, request, awsCredentialsProvider);

        init(request);

        final UndeployResponse response = ctx.getDeployer(request.getApplication()).undeploy(request);
        response.setTransactionId(request.getTransactionId());
        return response;
    }

    @RequestMapping(value = "/artifact/{name}/{version}", method = RequestMethod.POST)
    @ApiOperation(value = "Upload an application artifact")
    public UploadArtifactResponse artifact(
            @PathVariable @ApiParam(value = "Name of the application") final String name,
            @PathVariable @ApiParam(value = "Version of the application") final String version,
            @RequestParam("file") @ApiParam(value = "Artifact in .zip format.") final MultipartFile file
    ) throws IOException {
        final String filename = file.getOriginalFilename();
        final File localFile = File.createTempFile(filename, null);
        try {

            /* Store the file locally */
            LOGGER.info("Storing {} to {}", filename, localFile.getAbsolutePath());
            try (final FileOutputStream fout = new FileOutputStream(localFile)) {
                IOUtils.copy(file.getInputStream(), fout);
            }

            /* Extract application descriptor */
            LOGGER.info("Extracting application descriptor");
            boolean found = false;
            try (final FileInputStream fin = new FileInputStream(localFile)) {
                final ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    if (isApplicationDescriptor(ze)) {
                        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
                        IOUtils.copy(zin, bout);

                        storageService.put(descriptorPath(name), new ByteArrayInputStream(bout.toByteArray()));
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                throw new KribiException(KribiException.ARTIFACT_MALFORMED, "Unable to find .kribi.json in the artifact");
            }

            /* Store */
            try (final FileInputStream fin = new FileInputStream(localFile)) {
                final String ext = FilenameUtils.getExtension(filename);
                final String path = String.format("repository/%s/%s/%s.%s", name, version, name, ext);

                LOGGER.info("Archiving {}", path);
                storageService.put(path, fin);
            }

            final UploadArtifactResponse response = new UploadArtifactResponse();
            response.setTransactionId(transactionIdGenerator.get());
            return response;

        } finally {
            LOGGER.info("Deleting {}", localFile.getAbsolutePath());
            localFile.delete();
        }
    }

    @ExceptionHandler(KribiException.class)
    public ResponseEntity<ErrorResponse> onKribiException(final KribiException ex){
        final ErrorResponse resp = new ErrorResponse(ex.getCode(), ex.getMessage());
        resp.setTransactionId(transactionIdGenerator.get());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(resp);
    }

    //-- Private
    private String descriptorPath(final String name){
        return String.format("repository/%s/application.json", name, "application.json");
    }

    private boolean isApplicationDescriptor(final ZipEntry ze) {
        return ".kribi.json".equals(ze.getName()) || "BOOT-INF/classes/.kribi.json".equals(ze.getName());
    }

    private void init(final KribiRequest request) throws IOException {
        request.setTransactionId(transactionIdGenerator.get());
        request.setApplication(loadApplicationDescriptor(request.getApplicationName()));
    }

    private Application loadApplicationDescriptor(final String name) throws IOException {
        // Load descriptor
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            storageService.get(descriptorPath(name), out);
        } catch (final Exception e) {
            throw new KribiException(KribiException.APPLICATION_DESCRIPTOR_NOT_FOUND, "Unable to resolve the application descriptor", e);
        }

        // Deserialize
        try {
            return objectMapperBuilder.build().readValue(out.toByteArray(), Application.class);
        } catch (final JsonMappingException e) {
            throw new KribiException(KribiException.APPLICATION_DESCRIPTOR_MALFORMED, "Application descriptor is not valid JSON file", e);
        }
    }
}
