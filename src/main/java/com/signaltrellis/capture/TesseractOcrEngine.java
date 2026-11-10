package com.signaltrellis.capture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class TesseractOcrEngine implements OcrEngine {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final int MAX_OUTPUT_BYTES = 1_000_000;
    private final String executable;
    private final String languages;

    public TesseractOcrEngine(
            @Value("${signaltrellis.ocr.tesseract-path:tesseract}") String executable,
            @Value("${signaltrellis.ocr.languages:eng}") String languages) {
        this.executable = executable;
        this.languages = languages;
    }

    @Override
    public String extractText(byte[] imageBytes) throws IOException {
        Process process;
        try {
            process = new ProcessBuilder(executable, "stdin", "stdout", "-l", languages, "--psm", "3")
                    .start();
        } catch (IOException exception) {
            throw new OcrUnavailableException();
        }

        CompletableFuture<byte[]> stderr = CompletableFuture.supplyAsync(
                () -> readBounded(process.getErrorStream(), 8192));
        CompletableFuture<byte[]> stdout = CompletableFuture.supplyAsync(
                () -> readBounded(process.getInputStream(), MAX_OUTPUT_BYTES + 1));

        try {
            try (var output = process.getOutputStream()) {
                output.write(imageBytes);
            }
            boolean finished = process.waitFor(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("OCR process exceeded its time limit");
            }
            byte[] result = stdout.join();
            if (result.length > MAX_OUTPUT_BYTES) {
                throw new IOException("OCR output exceeded the extraction limit");
            }
            byte[] error = stderr.join();
            if (process.exitValue() != 0) {
                String message = new String(error, java.nio.charset.StandardCharsets.UTF_8).trim();
                throw new IOException(message.isEmpty() ? "Tesseract could not process the image" : message);
            }
            return new String(result, java.nio.charset.StandardCharsets.UTF_8);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("OCR was interrupted", exception);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private byte[] readBounded(InputStream stream, int maximumBytes) {
        try (InputStream input = stream; var output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() < maximumBytes) {
                    output.write(buffer, 0, Math.min(read, maximumBytes - output.size()));
                }
            }
            return output.toByteArray();
        } catch (IOException exception) {
            return new byte[0];
        }
    }
}
