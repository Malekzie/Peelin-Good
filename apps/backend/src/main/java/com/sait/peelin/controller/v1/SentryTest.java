package com.sait.peelin.controller.v1;

import io.sentry.Sentry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SentryTest {
    @GetMapping("/test-error")
    public String testError() {
        try {
            throw new RuntimeException("Something Broke!");
        } catch (Exception e) {
            Sentry.captureException(e);
            return "Sentry test error";
        }
    }

    @GetMapping("/unhandled")
    public String unhandled() {
        throw new RuntimeException("Unhandled exception");
    }

}
