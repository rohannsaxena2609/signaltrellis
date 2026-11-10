package com.signaltrellis.capture;

import java.io.IOException;

public interface OcrEngine {
    String extractText(byte[] imageBytes) throws IOException;
}
