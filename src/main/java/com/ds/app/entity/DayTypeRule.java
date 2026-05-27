package com.ds.app.entity;

import com.ds.app.enums.CompensationMode;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

@Embeddable
@Data
public class DayTypeRule {

    @Enumerated(EnumType.STRING)
    private CompensationMode mode;

    // Minimum minutes the employee must work extra to get ANYTHING (e.g., 60 mins)
    private Integer minMinutesToQualify;

    // The multipliers for THIS specific day type
    private Double cashMultiplier;      // e.g., 1.5x
    private Double compOffMultiplier;   // e.g., 1.0x

    // Only used if mode == POLICY_SPLIT (e.g., 480 minutes / 8 hours)
    private Integer maxCompOffMinutesBeforeSplit;
}