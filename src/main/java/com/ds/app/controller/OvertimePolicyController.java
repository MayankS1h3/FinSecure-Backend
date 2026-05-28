package com.ds.app.controller;

import com.ds.app.dto.request.OvertimePolicyRequest;
import com.ds.app.dto.response.OvertimePolicyResponse;
import com.ds.app.service.IOvertimePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/overtime-policies")
@RequiredArgsConstructor
public class OvertimePolicyController {

    private final IOvertimePolicyService overtimePolicyService;

    @PreAuthorize("hasAnyAuthority('HR','ADMIN')")
    @GetMapping
    public ResponseEntity<List<OvertimePolicyResponse>> getPolicies(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state
    ) {
        return ResponseEntity.ok(overtimePolicyService.getPolicies(country, state));
    }

    @PreAuthorize("hasAnyAuthority('HR','ADMIN')")
    @GetMapping("/{policyId}")
    public ResponseEntity<OvertimePolicyResponse> getPolicy(@PathVariable Long policyId) {
        return ResponseEntity.ok(overtimePolicyService.getPolicy(policyId));
    }

    @PreAuthorize("hasAnyAuthority('HR','ADMIN')")
    @PostMapping
    public ResponseEntity<OvertimePolicyResponse> createPolicy(
            @Valid @RequestBody OvertimePolicyRequest request
    ) {
        OvertimePolicyResponse response = overtimePolicyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('HR','ADMIN')")
    @PutMapping("/{policyId}")
    public ResponseEntity<OvertimePolicyResponse> updatePolicy(
            @PathVariable Long policyId,
            @Valid @RequestBody OvertimePolicyRequest request
    ) {
        return ResponseEntity.ok(overtimePolicyService.updatePolicy(policyId, request));
    }
}
