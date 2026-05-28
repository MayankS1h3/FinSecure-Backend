package com.ds.app.service;

import com.ds.app.dto.request.OvertimePolicyRequest;
import com.ds.app.dto.response.OvertimePolicyResponse;

import java.util.List;

public interface IOvertimePolicyService {

    List<OvertimePolicyResponse> getPolicies(String country, String state);

    OvertimePolicyResponse getPolicy(Long policyId);

    OvertimePolicyResponse createPolicy(OvertimePolicyRequest request);

    OvertimePolicyResponse updatePolicy(Long policyId, OvertimePolicyRequest request);
}
