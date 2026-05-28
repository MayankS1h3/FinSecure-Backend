package com.ds.app.dto.request;

import com.ds.app.enums.CompensationMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DayTypeRuleRequest {

    @NotNull(message = "Compensation mode is required")
    private CompensationMode mode;

    @Min(value = 0, message = "Minimum minutes must be zero or greater")
    private Integer minMinutesToQualify;

    @DecimalMin(value = "0.0", message = "Cash multiplier must be zero or greater")
    private Double cashMultiplier;

    @DecimalMin(value = "0.0", message = "Comp-off multiplier must be zero or greater")
    private Double compOffMultiplier;

    @Min(value = 0, message = "Max comp-off minutes must be zero or greater")
    private Integer maxCompOffMinutesBeforeSplit;
}
