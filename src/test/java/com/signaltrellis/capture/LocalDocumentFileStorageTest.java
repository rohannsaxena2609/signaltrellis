package com.signaltrellis.capture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LocalDocumentFileStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesAndOpensBinaryContentWithoutLeavingTemporaryFiles() throws IOException {
        Path storageDirectory = temporaryDirectory.resolve("documents");
        LocalDocumentFileStorage storage = new LocalDocumentFileStorage(storageDirectory.toString());
        UUID storageKey = UUID.randomUUID();
        byte[] content = {0, 1, 2, (byte) 0x80, (byte) 0xff};

        storage.store(storageKey, content);

        try (InputStream stored = storage.open(storageKey)) {
            assertArrayEquals(content, stored.readAllBytes());
        }
        try (Stream<Path> files = Files.list(storageDirectory)) {
            assertEquals(List.of(storageKey.toString()),
                    files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void deleteRemovesStoredContentAndCanBeRepeated() throws IOException {
        LocalDocumentFileStorage storage = new LocalDocumentFileStorage(temporaryDirectory.toString());
        UUID storageKey = UUID.randomUUID();
        storage.store(storageKey, new byte[]{1, 2, 3});

        storage.delete(storageKey);

        assertFalse(Files.exists(temporaryDirectory.resolve(storageKey.toString())));
        assertDoesNotThrow(() -> storage.delete(storageKey));
    }
}
