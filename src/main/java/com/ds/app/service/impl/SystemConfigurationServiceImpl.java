package com.ds.app.service.impl;

import com.ds.app.dto.request.ConfigRequest;
import com.ds.app.dto.response.ConfigResponse;
import com.ds.app.entity.SystemConfiguration;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.ConfigMapper;
import com.ds.app.repository.ISystemConfigurationRepository;
import com.ds.app.service.ISystemConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemConfigurationServiceImpl implements ISystemConfigurationService {

    private final ISystemConfigurationRepository configRepo;
    private final ConfigMapper configMapper;

    @Value("${app.MAX_HOURS_PER_DAY}")
    private int MAX_HOURS_PER_DAY;

    @Override
    public ConfigResponse updateConfig(String configKey, ConfigRequest configRequest) {
        SystemConfiguration config = configRepo.findById(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("System configuration not found"));

        config.setConfigValue(configRequest.getConfigValue());
        SystemConfiguration updatedConfig = configRepo.save(config);

        return configMapper.mapToResponse(updatedConfig);
    }

    @Override
    public SystemConfiguration getConfigByKey(String configKey) {

        return configRepo.findById(configKey)
                .orElseThrow(() -> new ResourceNotFoundException("System configuration not found"));
    }

    @Override
    public List<ConfigResponse> getAllConfigs() {
        List<SystemConfiguration> configs = configRepo.findAll();

        return configs.stream()
                .map(config -> configMapper.mapToResponse(config))
                .toList();
    }

    @Override
    public int getMaxHoursPerDay() {
        SystemConfiguration config = getConfigByKey("MAX_HOURS_PER_DAY");

        try{
            return Integer.parseInt(config.getConfigValue());
        }catch (Exception e){
            e.printStackTrace();
        }

        return MAX_HOURS_PER_DAY;
    }
}
