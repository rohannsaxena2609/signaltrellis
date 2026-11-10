package com.signaltrellis.capture;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface DocumentFileStorage {
    void store(UUID storageKey, byte[] content) throws IOException;
    InputStream open(UUID storageKey) throws IOException;
    void delete(UUID storageKey) throws IOException;
}
