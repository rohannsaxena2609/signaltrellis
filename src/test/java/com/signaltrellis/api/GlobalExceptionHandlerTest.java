package com.signaltrellis.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void malformedJsonReturnsSafeClientError() {
        var input = new MockHttpInputMessage("{not-json".getBytes(StandardCharsets.UTF_8));
        var exception = new HttpMessageNotReadableException("Parser detail", input);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/register");

        var response = handler.handleMalformedRequest(exception, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("MALFORMED_REQUEST", body.code());
        assertEquals("Request body could not be read", body.message());
        assertEquals("/api/v1/auth/register", body.path());
        assertFalse(body.message().contains("Parser detail"));
    }

    @Test
    void unexpectedErrorDoesNotExposeExceptionDetails() {
        var request = new MockHttpServletRequest("GET", "/api/v1/private");

        var response = handler.handleUnexpected(new IllegalStateException("sensitive diagnostic"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.code());
        assertEquals("An unexpected error occurred", body.message());
        assertFalse(body.message().contains("sensitive diagnostic"));
    }
}
