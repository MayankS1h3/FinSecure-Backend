package com.ds.app.service.impl;

import com.ds.app.dto.request.OvertimePolicyRequest;
import com.ds.app.dto.response.OvertimePolicyResponse;
import com.ds.app.entity.OvertimePolicy;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.OvertimePolicyMapper;
import com.ds.app.repository.IOvertimePolicyRepository;
import com.ds.app.service.IOvertimePolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OvertimePolicyServiceImpl implements IOvertimePolicyService {

    private final IOvertimePolicyRepository overtimePolicyRepository;
    private final OvertimePolicyMapper overtimePolicyMapper;

    @Override
    public List<OvertimePolicyResponse> getPolicies(String country, String state) {
        List<OvertimePolicy> policies;

        if (country != null && state != null) {
            policies = overtimePolicyRepository.findByCountryAndState(country, state);
        } else if (country != null) {
            policies = overtimePolicyRepository.findByCountry(country);
        } else if (state != null) {
            policies = overtimePolicyRepository.findByState(state);
        } else {
            policies = overtimePolicyRepository.findAll();
        }

        return policies.stream()
                .map(overtimePolicyMapper::mapToResponse)
                .toList();
    }

    @Override
    public OvertimePolicyResponse getPolicy(Long policyId) {
        OvertimePolicy policy = overtimePolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime policy not found"));
        return overtimePolicyMapper.mapToResponse(policy);
    }

    @Override
    @Transactional
    public OvertimePolicyResponse createPolicy(OvertimePolicyRequest request) {
        if (overtimePolicyRepository.existsByCountryAndStateAndName(
                request.getCountry(), request.getState(), request.getName())) {
            throw new IllegalStateException("Overtime policy already exists for the specified region and name");
        }

        OvertimePolicy policy = overtimePolicyMapper.mapToEntity(request);
        OvertimePolicy saved = overtimePolicyRepository.save(policy);
        return overtimePolicyMapper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public OvertimePolicyResponse updatePolicy(Long policyId, OvertimePolicyRequest request) {
        OvertimePolicy policy = overtimePolicyRepository.findById(policyId)
                .orElseThrow(() -> new ResourceNotFoundException("Overtime policy not found"));

        Optional<OvertimePolicy> existing = overtimePolicyRepository.findByCountryAndStateAndName(
                request.getCountry(), request.getState(), request.getName());
        if (existing.isPresent() && !existing.get().getPolicyId().equals(policyId)) {
            throw new IllegalStateException("Overtime policy already exists for the specified region and name");
        }

        overtimePolicyMapper.updateEntity(policy, request);
        OvertimePolicy saved = overtimePolicyRepository.save(policy);
        return overtimePolicyMapper.mapToResponse(saved);
    }
}
