package com.ds.app.controller;

import com.ds.app.dto.request.ConfigRequest;
import com.ds.app.dto.response.ConfigResponse;
import com.ds.app.service.ISystemConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("system-config")
@RequiredArgsConstructor
public class SystemConfigurationController {

    private final ISystemConfigurationService configService;

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping()
    public ResponseEntity<List<ConfigResponse>> getAllConfigs() {
        List<ConfigResponse> responseList = configService.getAllConfigs();
        return ResponseEntity.ok(responseList);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{configKey}")
    public ResponseEntity<ConfigResponse> updateConfig(
            @PathVariable String configKey,
            @RequestBody @Valid  ConfigRequest configRequest
    ) {
        ConfigResponse response = configService.updateConfig(configKey, configRequest);
        return ResponseEntity.ok(response);
    }
}
