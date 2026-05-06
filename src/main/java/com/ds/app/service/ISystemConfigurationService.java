package com.ds.app.service;

import com.ds.app.dto.request.ConfigRequest;
import com.ds.app.dto.response.ConfigResponse;
import com.ds.app.entity.SystemConfiguration;

import java.util.List;

public interface ISystemConfigurationService {
    ConfigResponse updateConfig(String configKey, ConfigRequest configRequest);

    SystemConfiguration getConfigByKey(String configKey);

    List<ConfigResponse> getAllConfigs();

    int getMaxHoursPerDay();
}
