package io.tchepannou.kribi.aws.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.kribi.services.StorageService;
import jdk.nashorn.internal.ir.annotations.Ignore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class S3AccountRepositoryTest {
    @Mock
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private S3AccountRepository repository;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        when(objectMapperBuilder.build()).thenReturn(objectMapper);
    }

    @Test
    @Ignore
    public void testFindAccount() throws Exception {
    }

    @Test
    @Ignore
    public void testUpdate() throws Exception {

    }
}
