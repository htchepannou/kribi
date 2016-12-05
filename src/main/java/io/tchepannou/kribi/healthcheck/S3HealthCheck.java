package io.tchepannou.kribi.healthcheck;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class S3HealthCheck implements HealthIndicator {
    private final String bucket;
    private final AmazonS3 s3;

    public S3HealthCheck(final String bucket, final AmazonS3 s3) {
        this.bucket = bucket;
        this.s3 = s3;
    }

    @Override
    public Health health() {
        final long start = System.currentTimeMillis();
        try {

            final ObjectListing list = s3.listObjects(bucket);
            final long latency = System.currentTimeMillis() - start;

            return Health
                    .up()
                    .withDetail("bucket", bucket)
                    .withDetail("latency", latency)
                    .withDetail("objectCount", list.getObjectSummaries().size())
                    .build();

        } catch (final Exception ex) {
            final long latency = System.currentTimeMillis() - start;
            return Health
                    .down()
                    .withDetail("bucket", bucket)
                    .withDetail("latency", latency)
                    .withException(ex)
                    .build();

        }
    }
}
