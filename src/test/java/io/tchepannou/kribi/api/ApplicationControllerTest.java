package io.tchepannou.kribi.api;

import com.amazonaws.auth.AWSCredentialsProvider;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.aws.AwsContext;
import io.tchepannou.kribi.aws.AwsContextFactory;
import io.tchepannou.kribi.client.ArtifactResponse;
import io.tchepannou.kribi.client.DeployResponse;
import io.tchepannou.kribi.client.ErrorResponse;
import io.tchepannou.kribi.client.ReleaseResponse;
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
import io.tchepannou.kribi.services.TransactionIdGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
    Account acc;

    @InjectMocks
    ApplicationController controller;

    String transactionId;

    @Before
    public void setUp() throws Exception {
        when(context.getDeployer(any())).thenReturn(deployer);
        when(awsContextFactory.create(any(), any(), any())).thenReturn(context);

        transactionId = UUID.randomUUID().toString();
        when(transactionIdGenerator.get()).thenReturn(transactionId);

        when(accountRepository.findAccount()).thenReturn(acc);
    }

    //-- Test
    @Test
    public void shouldDeploy() throws Exception {
        // Given
        final DeployResponse response = new DeployResponse(new Cluster());
        when(deployer.deploy(any())).thenReturn(response);

        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        // When
        final DeployResponse result = controller.deploy("foo", "1.1", "int", "us-west-1", false, false);

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }

    @Test(expected = KribiException.class)
    public void shouldNotDeployInvalidApplication() throws Exception {
        // Given
        when(applicationDescriptorService.isValid(any(), any())).thenReturn(false);

        // When
        controller.deploy("foo", "1.1", "int", "us-west-1", false, false);
    }

    @Test
    public void shouldUndeploy() throws Exception {
        // Given
        final UndeployResponse response = new UndeployResponse();
        when(deployer.undeploy(any())).thenReturn(response);

        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        // When
        final UndeployResponse result = controller.undeploy("foo", "1.1", "int", "us-west-1");

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);
    }


    @Test
    public void shouldRelease() throws Exception {
        // Given
        final ReleaseResponse response = new ReleaseResponse();
        when(deployer.release(any())).thenReturn(response);

        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        // When
        final ReleaseResponse result = controller.release("foo", "1.1", "int", "us-west-1", false);

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getApplicationName()).isEqualTo("foo");
        assertThat(result.getEnvironment()).isEqualTo(Environment.INT);
        assertThat(result.getRegion()).isEqualTo("us-west-1");
        assertThat(result.getVersion()).isEqualTo("1.1");
        assertThat(result.getUndeployResponses()).isEmpty();
        assertThat(result.getUndeployErrors()).isEmpty();
    }

    @Test
    public void shouldReleaseAndDeleteOld() throws Exception {
        // Given
        final ReleaseResponse response = new ReleaseResponse();
        when(deployer.release(any())).thenReturn(response);
        when(deployer.getVersions(any(), any())).thenReturn(Arrays.asList("1.0", "1.1", "1.1.1", "1.2"));
        when(deployer.undeploy(any())).thenReturn(new UndeployResponse());


        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        ApplicationController ctl = spy(controller);

        // When
        final ReleaseResponse result = ctl.release("foo", "1.2", "int", "us-west-1", true);

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);

        verify(ctl).undeploy("foo", "1.0", "int", "us-west-1");
        verify(ctl).undeploy("foo", "1.1", "int", "us-west-1");
        verify(ctl).undeploy("foo", "1.1.1", "int", "us-west-1");

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getApplicationName()).isEqualTo("foo");
        assertThat(result.getEnvironment()).isEqualTo(Environment.INT);
        assertThat(result.getRegion()).isEqualTo("us-west-1");
        assertThat(result.getVersion()).isEqualTo("1.2");
        assertThat(result.getUndeployResponses()).hasSize(3);
        assertThat(result.getUndeployErrors()).isEmpty();
    }


    @Test
    public void shouldReleaseAndDeleteOldWithError() throws Exception {
        // Given
        final ReleaseResponse response = new ReleaseResponse();
        when(deployer.release(any())).thenReturn(response);
        when(deployer.getVersions(any(), any())).thenReturn(Arrays.asList("1.0", "1.1", "1.1.1", "1.2"));

        final KribiException ex = new KribiException("error", "message");
        when(deployer.undeploy(any()))
                .thenReturn(new UndeployResponse())
                .thenThrow(ex)
                .thenReturn(new UndeployResponse())
                .thenReturn(new UndeployResponse())
                .thenReturn(new UndeployResponse())
        ;


        final Application app = createApplication();
        when(applicationDescriptorService.load(any())).thenReturn(app);

        when(applicationDescriptorService.isValid(any(), any())).thenReturn(true);

        final ApplicationController ctl = spy(controller);

        // When
        final ReleaseResponse result = ctl.release("foo", "1.2", "int", "us-west-1", true);

        // Then
        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result).isEqualTo(response);

        verify(ctl).undeploy("foo", "1.0", "int", "us-west-1");
        verify(ctl).undeploy("foo", "1.1", "int", "us-west-1");
        verify(ctl).undeploy("foo", "1.1.1", "int", "us-west-1");

        assertThat(result.getTransactionId()).isEqualTo(transactionId);
        assertThat(result.getApplicationName()).isEqualTo("foo");
        assertThat(result.getEnvironment()).isEqualTo(Environment.INT);
        assertThat(result.getRegion()).isEqualTo("us-west-1");
        assertThat(result.getVersion()).isEqualTo("1.2");
        assertThat(result.getUndeployResponses()).hasSize(2);
        assertThat(result.getUndeployErrors()).hasSize(1);

    }


    @Test(expected = KribiException.class)
    public void shouldNotReleaseInvalidApplication() throws Exception {
        // Given
        when(applicationDescriptorService.isValid(any(), any())).thenReturn(false);

        // When
        controller.release("foo", "1.1", "int", "us-west-1", false);
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

    @Test
    public void shouldReturn404OnArtifactNotFound(){
        KribiException ex = new KribiException(KribiException.ARTIFACT_NOT_FOUND, "text");

        ResponseEntity<ErrorResponse> result = controller.onKribiException(ex);

        assertThat(result.getStatusCode().value()).isEqualTo(404);
        assertThat(result.getBody().getCode()).isEqualTo(ex.getCode());
        assertThat(result.getBody().getMessage()).isEqualTo(ex.getMessage());
    }

    @Test
    public void shouldReturn409OnKribiNotFound(){
        KribiException ex = new KribiException("any-error", "text");

        ResponseEntity<ErrorResponse> result = controller.onKribiException(ex);

        assertThat(result.getStatusCode().value()).isEqualTo(409);
        assertThat(result.getBody().getCode()).isEqualTo(ex.getCode());
        assertThat(result.getBody().getMessage()).isEqualTo(ex.getMessage());
    }

    //-- Private
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
