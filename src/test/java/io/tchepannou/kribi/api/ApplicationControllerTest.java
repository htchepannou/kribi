package io.tchepannou.kribi.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.client.UploadArtifactResponse;
import io.tchepannou.kribi.client.DeployRequest;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.UndeployRequest;
import io.tchepannou.kribi.client.UndeployResponse;
import io.tchepannou.kribi.model.Account;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.Cluster;
import io.tchepannou.kribi.model.Environment;
import io.tchepannou.kribi.model.OS;
import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import io.tchepannou.kribi.services.AccountRepository;
import io.tchepannou.kribi.services.Deployer;
import io.tchepannou.kribi.services.Installer;
import io.tchepannou.kribi.services.StorageService;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationControllerTest {
    @Mock
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Mock
    AWSCredentialsProvider awsCredentialsProvider;

    @Mock
    AccountRepository accountRepository;

    @Mock
    AwsContextFactory awsContextFactory;

    @Mock
    StorageService storageService;

    @Mock
    AwsContext context;

    @Mock
    Deployer deployer;

    @Mock
    TransactionIdGenerator transactionIdGenerator;

    @Mock
    Installer installer;

    @InjectMocks
    ApplicationController controller;

    String transactionId;

    @Before
    public void setUp() throws Exception {
        when(context.getDeployer(any())).thenReturn(deployer);
        when(context.getInstaller(any())).thenReturn(installer);

        when(awsContextFactory.create(any(), any(), any())).thenReturn(context);

        when(objectMapperBuilder.build()).thenReturn(new ObjectMapper());

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final InputStream in = getClass().getResourceAsStream("/api/ApplicationController/.kribi.json");
                final OutputStream out = (OutputStream) invocationOnMock.getArguments()[1];
                return IOUtils.copy(in, out);
            }
        }).when(storageService).get(any(), any());

        transactionId = UUID.randomUUID().toString();
        when(transactionIdGenerator.get()).thenReturn(transactionId);
    }

    //-- Test
    @Test
    public void shouldDeploy() throws Exception {
        // Given
        final Account acc = createAccount("shouldDeploy");
        when(accountRepository.findAccount()).thenReturn(acc);

        final DeployRequest request = createDeployRequest();
        final DeployResponse response = new DeployResponse(new Cluster());
        when(deployer.deploy(request)).thenReturn(response);

        // When
        final DeployResponse result = controller.deploy(request);

        // Then
        assertThat(request.getTransactionId()).isEqualTo(transactionId);
        assertThat(request.getApplication()).isNotNull();

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }

    @Test
    public void shouldUndeploy() throws Exception {
        // Given
        final Account acc = createAccount("shouldUndeploy");
        when(accountRepository.findAccount()).thenReturn(acc);

        final UndeployRequest request = createUndeployRequest();
        final UndeployResponse response = new UndeployResponse();
        when(deployer.undeploy(request)).thenReturn(response);

        // When
        final UndeployResponse result = controller.undeploy(request);

        // Then
        assertThat(request.getTransactionId()).isEqualTo(transactionId);
        assertThat(request.getApplication()).isNotNull();

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }


    @Test
    public void shouldUploadArtifact() throws Exception {
        // Given
        final InputStream in = getClass().getResourceAsStream("/api/ApplicationController/gs-rest-service-0.1.0.jar");
        final MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("gs-rest-service-1.1.jar");
        when(file.getInputStream()).thenReturn(in);

        // When
        final UploadArtifactResponse result = controller.artifact("gs-rest-service", "1.1", file);

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        verify(storageService).put(eq("repository/gs-rest-service/application.json"), any(InputStream.class));
        verify(storageService).put(eq("repository/gs-rest-service/1.1/gs-rest-service.jar"), any(InputStream.class));
    }



    @Test(expected = KribiException.class)
    public void shouldThrowExceptionWhenUploadingNonZipArtifact() throws Exception {
        // Given
        final InputStream in = getClass().getResourceAsStream("/api/ApplicationController/invalid-artifact-format.txt");
        final MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("invalid-artifact-format.txt");
        when(file.getInputStream()).thenReturn(in);

        // When
        controller.artifact("foo", "1.0", file);
    }

    //-- Private
    private Account createAccount(final String name) {
        final Account acc = new Account();
        acc.setName(name);
        acc.getKeyPair().setName("kp-" + name);
        acc.getKeyPair().setPrivateKey("PK---" + name);
        acc.getLoadBalancer().setHttpPort(111);
        acc.getLoadBalancer().setHttpsPort(222);
        acc.getSecurityGroups().setHttp("http");
        acc.getSecurityGroups().setHttps("https");
        acc.getSecurityGroups().setSsh("ssh");
        return acc;
    }

    private DeployRequest createDeployRequest() {
        final DeployRequest request = new DeployRequest();
        request.setEnvironment(Environment.INT);
        request.setRegion("us-west-2");
        request.setVersion("v1");
        request.setApplicationName("app");
        return request;
    }

    private UndeployRequest createUndeployRequest() {
        final UndeployRequest request = new UndeployRequest();
        request.setEnvironment(Environment.INT);
        request.setRegion("us-west-2");
        request.setVersion("v1");
        request.setApplicationName("app");
        return request;
    }

    private Application createApplication() {
        final Application app = new Application();
        final String name = "app-" + System.currentTimeMillis();

        app.setDescription("Description of " + name);
        app.setHealthCheckPath("/health");
        app.setName(name);
        app.setOperatingSystem(OS.LINUX);
        app.setTemplate(ApplicationTemplate.springboot);

        return app;
    }

}
