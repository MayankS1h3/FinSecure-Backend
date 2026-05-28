package com.ds.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimePolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String name;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "State is required")
    private String state;

    @NotNull(message = "Standard daily minutes is required")
    @Min(value = 1, message = "Standard daily minutes must be at least 1")
    private Integer standardDailyMinutes;

    @NotNull(message = "Standard weekly minutes is required")
    @Min(value = 1, message = "Standard weekly minutes must be at least 1")
    private Integer standardWeeklyMinutes;

    @NotNull(message = "Weekday rule is required")
    @Valid
    private DayTypeRuleRequest weekdayRule;

    @NotNull(message = "Weekend rule is required")
    @Valid
    private DayTypeRuleRequest weekendRule;

    @NotNull(message = "Holiday rule is required")
    @Valid
    private DayTypeRuleRequest holidayRule;
}
