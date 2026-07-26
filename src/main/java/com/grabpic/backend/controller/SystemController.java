package com.grabpic.backend.controller;

import com.grabpic.backend.dto.response.SystemStatusResponse;
import com.grabpic.backend.service.SystemStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemStatusService systemStatusService;

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> getSystemStatus() {
        return ResponseEntity.ok(systemStatusService.getSystemStatus());
    }
}
