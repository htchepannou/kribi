package io.tchepannou.kribi.healthcheck;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3HealthCheckTest {
    private static final String BUCKET = "bucket";

    @Mock
    AmazonS3 s3;

    @Mock
    ObjectListing list;

    S3HealthCheck healthCheck;


    @Before
    public void setUp(){
        healthCheck = new S3HealthCheck(BUCKET, s3);
    }

    @Test
    public void shouldBeUp(){
        when(list.getObjectSummaries()).thenReturn(Arrays.asList(
                mock(S3ObjectSummary.class),
                mock(S3ObjectSummary.class),
                mock(S3ObjectSummary.class)
        ));
        when(s3.listObjects(BUCKET)).thenReturn(list);

        final Health result = healthCheck.health();

        assertThat(result.getStatus()).isEqualTo(Status.UP);
        assertThat(result.getDetails()).containsKey("latency");
        assertThat(result.getDetails()).containsEntry("bucket", BUCKET);
        assertThat(result.getDetails()).containsEntry("objectCount", 3);
        assertThat(result.getDetails()).doesNotContainKeys("error");

    }

    @Test
    public void shouldBeDownWhenS3Exception(){
        final RuntimeException ex = new RuntimeException("error");
        when(s3.listObjects(BUCKET)).thenThrow(ex);

        final Health result = healthCheck.health();

        assertThat(result.getStatus()).isEqualTo(Status.DOWN);
        assertThat(result.getDetails()).containsKey("latency");
        assertThat(result.getDetails()).containsKey("error");
        assertThat(result.getDetails()).containsEntry("bucket", BUCKET);

    }
}
