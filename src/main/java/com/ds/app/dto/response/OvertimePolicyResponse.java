package com.ds.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OvertimePolicyResponse {
    private Long policyId;
    private String name;
    private String country;
    private String state;
    private Integer standardDailyMinutes;
    private Integer standardWeeklyMinutes;
    private DayTypeRuleResponse weekdayRule;
    private DayTypeRuleResponse weekendRule;
    private DayTypeRuleResponse holidayRule;
}
