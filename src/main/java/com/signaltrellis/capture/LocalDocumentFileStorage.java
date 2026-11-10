package com.signaltrellis.capture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
public class LocalDocumentFileStorage implements DocumentFileStorage {

    private final Path root;

    public LocalDocumentFileStorage(@Value("${storage.documents.directory}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public void store(UUID storageKey, byte[] content) throws IOException {
        Files.createDirectories(root);
        Path destination = pathFor(storageKey);
        Path temporary = Files.createTempFile(root, "upload-", ".part");
        try {
            Files.write(temporary, content);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public InputStream open(UUID storageKey) throws IOException {
        return Files.newInputStream(pathFor(storageKey));
    }

    @Override
    public void delete(UUID storageKey) throws IOException {
        Files.deleteIfExists(pathFor(storageKey));
    }

    private Path pathFor(UUID storageKey) {
        Path path = root.resolve(storageKey.toString()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}
