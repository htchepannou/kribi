package io.tchepannou.kribi.aws.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.util.IOUtils;
import io.tchepannou.kribi.services.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

@ConfigurationProperties("kribi.aws")
public class S3StorageService implements StorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3StorageService.class);

    String bucket;

    @Autowired
    AmazonS3 s3;

    @Autowired
    TransferManager transferManager;

    //-- StorageService overrides
    @Override
    public void put(final String path, final InputStream in) throws IOException {
        LOGGER.info("put s3://{}/{}", bucket, path);

        final Upload upload = transferManager.upload(bucket, path, in, new ObjectMetadata());
        try {
            upload.waitForUploadResult();
        } catch (final InterruptedException e) {
            throw new IOException("Interrupted", e);
        }
    }

    @Override
    public void get(final String path, final OutputStream out) throws IOException {
        LOGGER.info("get s3://{}/{}", bucket, path);

        try (final S3Object obj = s3.getObject(bucket, path)) {
            IOUtils.copy(obj.getObjectContent(), out);
        }
    }

    @Override
    public <R> R get(final String path, final Function<InputStream, R> consumer) throws IOException{
        LOGGER.info("get s3://{}/{}", bucket, path);

        try (final S3Object obj = s3.getObject(bucket, path)) {
            return consumer.apply(obj.getObjectContent());
        }
    }

    @Override
    public boolean exists(final String path){
        try (final S3Object obj = s3.getObject(bucket, path)) {
            return true;
        }catch(Exception e){
            return false;
        }
    }

    //-- Getter/Setter
    public String getBucket() {
        return bucket;
    }

    public void setBucket(final String bucket) {
        this.bucket = bucket;
    }
}
