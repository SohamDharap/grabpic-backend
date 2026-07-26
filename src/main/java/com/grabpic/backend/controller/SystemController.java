package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.SystemStatusResponse;
import com.grabpic.backend.service.SystemStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Slf4j
@RequiredArgsConstructor
public class SystemController {

    private final SystemStatusService systemStatusService;

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        log.info("Entering status endpoint");
        ResponseEntity<SystemStatusResponse> response = ResponseEntity.ok(systemStatusService.getSystemStatus());
        log.info("Status endpoint completed successfully");
        return response;
        
    }
}
