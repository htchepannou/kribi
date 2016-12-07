package io.tchepannou.kribi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tchepannou.kribi.model.Application;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.InputStream;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationDescriptorServiceTest {

    @Mock
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Mock
    StorageService storageService;

    @Mock
    Function<InputStream, Application> extractor;

    @InjectMocks
    ApplicationDescriptorService service;

    @Before
    public void setUp() {
        when(objectMapperBuilder.build()).thenReturn(new ObjectMapper());
    }

    @Test
    public void testExtractFromJar() throws Exception {
        // Given
        when(storageService.exists("repository/foo/1.1/foo.jar")).thenReturn(true);
        when(storageService.exists("repository/foo/1.1/foo.zip")).thenReturn(false);

        final Application app = new Application();
        when(storageService.get(eq("repository/foo/1.1/foo.jar"), any(Function.class))).thenReturn(app);

        // When
        final Application result = service.extract("foo", "1.1");

        // Then
        assertThat(result).isEqualTo(app);
    }

    @Test
    public void testExtractFromZip() throws Exception {
        // Given
        when(storageService.exists("repository/foo/1.1/foo.jar")).thenReturn(false);
        when(storageService.exists("repository/foo/1.1/foo.zip")).thenReturn(true);

        final Application app = new Application();
        when(storageService.get(eq("repository/foo/1.1/foo.zip"), any(Function.class))).thenReturn(app);

        // When
        final Application result = service.extract("foo", "1.1");

        // Then
        assertThat(result).isEqualTo(app);
    }

    @Test
    public void testStore() throws Exception {
        // Given
        final Application app = new Application();
        app.setName("foo");

        // When
        service.store(app);

        // Then
        verify(storageService).put(eq("repository/foo/application.json"), any());
    }

    @Test
    public void testLoad() throws Exception {
        // Given
        final Application app = new Application();
        when(storageService.get(eq("repository/foo/application.json"), any(Function.class))).thenReturn(app);

        // When
        final Application result = service.load("foo");

        // Then
        assertThat(result).isEqualTo(app);
    }
}
