package io.tchepannou.kribi.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.client.ArtifactResponse;
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
import io.tchepannou.kribi.services.ApplicationDescriptorService;
import io.tchepannou.kribi.services.Deployer;
import io.tchepannou.kribi.services.Installer;
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationControllerTest {
    @Mock
    ApplicationDescriptorService applicationDescriptorService;

    @Mock
    AWSCredentialsProvider awsCredentialsProvider;

    @Mock
    AccountRepository accountRepository;

    @Mock
    AwsContextFactory awsContextFactory;

    @Mock
    AwsContext context;

    @Mock
    Deployer deployer;

    @Mock
    TransactionIdGenerator transactionIdGenerator;

    @Mock
    Installer installer;

    @Mock
    Account acc;

    @InjectMocks
    ApplicationController controller;

    String transactionId;

    @Before
    public void setUp() throws Exception {
        when(context.getDeployer(any())).thenReturn(deployer);
        when(context.getInstaller(any())).thenReturn(installer);
        when(awsContextFactory.create(any(), any(), any())).thenReturn(context);

        transactionId = UUID.randomUUID().toString();
        when(transactionIdGenerator.get()).thenReturn(transactionId);

        when(accountRepository.findAccount()).thenReturn(acc);
    }

    //-- Test
    @Test
    public void shouldDeploy() throws Exception {
        // Given
        final DeployRequest request = createDeployRequest();
        final DeployResponse response = new DeployResponse(new Cluster());
        when(deployer.deploy(request)).thenReturn(response);

        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        // When
        final DeployResponse result = controller.deploy(request);

        // Then
        assertThat(request.getTransactionId()).isEqualTo(transactionId);
        assertThat(request.getApplication()).isNotNull();

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }

    @Test(expected = KribiException.class)
    public void shouldNotDeployInvalidApplication() throws Exception {
        // Given
        when(applicationDescriptorService.isValid(any(), any())).thenReturn(false);

        // When
        controller.deploy(new DeployRequest());
    }

    @Test
    public void shouldUndeploy() throws Exception {
        // Given
        final UndeployRequest request = createUndeployRequest();
        final UndeployResponse response = new UndeployResponse();
        when(deployer.undeploy(request)).thenReturn(response);

        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        // When
        final UndeployResponse result = controller.undeploy(request);

        // Then
        assertThat(request.getTransactionId()).isEqualTo(transactionId);
        assertThat(request.getApplication()).isNotNull();

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }

    @Test(expected = KribiException.class)
    public void shouldNotUndeployInvalidApplication() throws Exception {
        // Given
        when(applicationDescriptorService.isValid(any(), any())).thenReturn(false);

        // When
        controller.undeploy(new UndeployRequest());
    }

    @Test
    public void showInitArtifact() throws Exception {
        // Given
        final Application app = createApplication();
        when(applicationDescriptorService.extract("gs-rest-service", "1.1")).thenReturn(app);

        // When
        final ArtifactResponse result = controller.initArtifact("gs-rest-service", "1.1");

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getApplication()).isEqualTo(app);
    }

    //-- Private
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
