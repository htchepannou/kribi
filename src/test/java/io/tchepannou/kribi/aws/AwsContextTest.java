package io.tchepannou.kribi.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.model.Application;
import io.tchepannou.kribi.model.OS;
import io.tchepannou.kribi.model.aws.ApplicationTemplate;
import io.tchepannou.kribi.model.aws.AwsAMI;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AwsContextTest {
    private static final String REGION = "us-west-2";
    private AwsContext context;
    private AWSCredentialsProvider credentialsProvider;
    private Application app;

    @Before
    public void setUp(){
        credentialsProvider = mock(AWSCredentialsProvider.class);

        app = new Application();
        app.setOperatingSystem(OS.LINUX);

        context = new AwsContext(credentialsProvider, REGION);
    }

    //-- getDeployer
    @Test
    public void getDeployerShouldReturnCloudFormationDeployer(){
        app.setTemplate(ApplicationTemplate.springboot);
        assertThat(context.getDeployer(app)).isInstanceOf(CloudFormationDeployer.class);
    }

    @Test
    public void getDeployerShouldReturnJavaAppDeployer(){
        app.setTemplate(ApplicationTemplate.javaapp);
        assertThat(context.getDeployer(app)).isInstanceOf(JavaAppDeployer.class);
    }

    @Test(expected = KribiException.class)
    public void getDeployerShouldThrowExceptionForUnknownTemplate(){
        app.setTemplate(ApplicationTemplate.none);
        context.getDeployer(app);
    }

    //-- getAPI
    @Test
    public void getAMIShouldReturnAmazonLinuxForLinuxApplication(){
        app.setOperatingSystem(OS.LINUX);
        assertThat(context.getAMI(app)).isEqualTo(AwsAMI.AMAZON_LINUX);
    }

    @Test(expected = KribiException.class)
    public void getAMIShouldThrowExceptionForWindowsApplication(){
        app.setOperatingSystem(OS.WINDOWS);
        context.getAMI(app);
    }
}
