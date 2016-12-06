package io.tchepannou.kribi.aws.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3StorageServiceTest {
    @Mock
    AmazonS3 s3;

    @Mock
    TransferManager transferManager;

    @InjectMocks
    S3StorageService service;

    @Before
    public void setUp() {
        service.setBucket("kribi");

    }

    @Test
    public void testPut() throws Exception {
        // Given
        final InputStream in = mock(InputStream.class);

        final Upload upload = mock(Upload.class);
        when(transferManager.upload(any(), any(), any(), any())).thenReturn(upload);

        // When
        service.put("/foo/bar.txt", in);

        // Then
        verify(upload).waitForUploadResult();
    }
}
