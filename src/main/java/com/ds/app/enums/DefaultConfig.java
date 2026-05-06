package com.ds.app.enums;

import java.util.Arrays;

public enum DefaultConfig {

    MAX_HOURS_PER_DAY("MAX_HOURS_PER_DAY", "540");

    private final String key;
    private final String defaultValue;

    DefaultConfig(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
