package io.tchepannou.kribi.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StorageService {
    void get(final String path, final OutputStream in) throws IOException;
    void put(final String path, final InputStream in) throws IOException;
}
