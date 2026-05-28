package com.ds.app.dto.response;

import com.ds.app.enums.CompensationMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayTypeRuleResponse {
    private CompensationMode mode;
    private Integer minMinutesToQualify;
    private Double cashMultiplier;
    private Double compOffMultiplier;
    private Integer maxCompOffMinutesBeforeSplit;
}
