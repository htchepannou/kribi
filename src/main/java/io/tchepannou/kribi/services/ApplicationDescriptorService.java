package io.tchepannou.kribi.services;

import io.tchepannou.kribi.KribiException;
import io.tchepannou.kribi.model.Application;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApplicationDescriptorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationDescriptorService.class);

    @Autowired
    Jackson2ObjectMapperBuilder objectMapperBuilder;

    @Autowired
    StorageService storageService;

    public Application extract(final String name, final String version) throws IOException {
        final String path = getArtifactPath(name, version);
        if (path == null) {
            throw new KribiException(KribiException.ARTIFACT_NOT_FOUND, "Unable to find " + name + " version " + version);
        }

        /* Extract application descriptor */
        LOGGER.info("Extracting application descriptor");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final Function<InputStream, Application> extractor = (fin) ->
        {
            try {
                final ZipInputStream zin = new ZipInputStream(fin);
                ZipEntry ze;
                while ((ze = zin.getNextEntry()) != null) {
                    if (isApplicationDescriptor(ze)) {
                        IOUtils.copy(zin, out);
                        break;
                    }
                }

                return objectMapperBuilder.build().readValue(out.toByteArray(), Application.class);
            } catch (final IOException e) {
                throw new KribiException(KribiException.ARTIFACT_MALFORMED, "Unable to extractDescriptor descriptor from artifact", e);
            }
        };

        return storageService.get(path, extractor);
    }

    public void store(final Application application) throws IOException {
        final String json = objectMapperBuilder.build().writeValueAsString(application);
        final String path = getDescriptorPath(application.getName());
        storageService.put(path, new ByteArrayInputStream(json.getBytes()));
    }

    public Application load(final String name) throws IOException {
        final String path = getDescriptorPath(name);
        final Function<InputStream, Application> extractor = (in) -> {
            try {
                return objectMapperBuilder.build().readValue(in, Application.class);
            } catch (IOException e) {
                throw new KribiException(KribiException.ARTIFACT_MALFORMED, "Unable to extractDescriptor descriptor from artifact", e);
            }
        };
        return storageService.get(path, extractor);
    }

    public boolean isValid(final String name, final String version){
        return getArtifactPath(name, version) != null;
    }

    //-- Private
    private String getDescriptorPath(final String name) {
        return String.format("repository/%s/application.json", name);
    }

    private String getArtifactPath(final String name, final String version) {
        String path = String.format("repository/%s/%s/%s.jar", name, version, name);
        if (!storageService.exists(path)) {
            path = String.format("repository/%s/%s/%s.zip", name, version, name);
            return storageService.exists(path) ? path : null;
        }

        return path;
    }

    private boolean isApplicationDescriptor(final ZipEntry ze) {
        return ".kribi.json".equals(ze.getName()) || "BOOT-INF/classes/.kribi.json".equals(ze.getName());
    }

}
