package io.tchepannou.kribi.aws.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class S3StorageServiceTest {
    @Mock
    AmazonS3 s3;

    @InjectMocks
    S3StorageService service;

    @Before
    public void setUp() {
        service.setBucket("kribi");
    }

    @Test
    public void testPut() throws Exception {
        final InputStream in = mock(InputStream.class);

        service.put("/foo/bar.txt", in);

        verify(s3).putObject(eq("kribi"), eq("/foo/bar.txt"), eq(in), any(ObjectMetadata.class));
    }
}
