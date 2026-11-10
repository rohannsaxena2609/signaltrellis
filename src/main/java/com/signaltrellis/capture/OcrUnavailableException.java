package com.signaltrellis.capture;

public class OcrUnavailableException extends RuntimeException {
    public OcrUnavailableException() {
        super("OCR is unavailable; install Tesseract or configure SIGNALTRELLIS_TESSERACT_PATH");
    }
}
