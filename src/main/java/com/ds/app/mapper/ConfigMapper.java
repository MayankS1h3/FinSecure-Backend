package com.ds.app.mapper;

import com.ds.app.dto.response.ConfigResponse;
import com.ds.app.entity.SystemConfiguration;
import org.springframework.stereotype.Component;

@Component
public class ConfigMapper {

    public ConfigResponse mapToResponse(SystemConfiguration config) {
        return ConfigResponse.builder()
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .build();
    }
}
