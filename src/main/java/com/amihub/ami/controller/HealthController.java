package com.amihub.ami.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller mapped to /api/sync/health
 * Render should be configured to check this path to keep the service active.
 */
@RestController
@RequestMapping("/api/sync")
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
