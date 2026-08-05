package com.ukgqtm.app.api;

import java.time.Instant;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1", produces = MediaType.APPLICATION_JSON_VALUE)
public class SystemController {
    @GetMapping("/health")
    public ResponseEntity<SystemStatusResponse> health() {
        return ResponseEntity.ok(SystemStatusResponse.up("health"));
    }

    @GetMapping("/ready")
    public ResponseEntity<SystemStatusResponse> ready() {
        return ResponseEntity.ok(SystemStatusResponse.up("ready"));
    }

    public record SystemStatusResponse(String status, String check, String service, Instant timestamp) {
        static SystemStatusResponse up(String check) {
            return new SystemStatusResponse("UP", check, "ukg-qa-test-management", Instant.now());
        }
    }
}
