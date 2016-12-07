package io.tchepannou.kribi.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;

public interface StorageService {
    boolean exists(final String path);

    @Deprecated
    void get(final String path, final OutputStream in) throws IOException;

    <R> R get(final String path, final Function<InputStream, R> consumer) throws IOException;
    void put(final String path, final InputStream in) throws IOException;


}
