package com.ds.app.mapper;

import com.ds.app.dto.request.DayTypeRuleRequest;
import com.ds.app.dto.request.OvertimePolicyRequest;
import com.ds.app.dto.response.DayTypeRuleResponse;
import com.ds.app.dto.response.OvertimePolicyResponse;
import com.ds.app.entity.DayTypeRule;
import com.ds.app.entity.OvertimePolicy;
import org.springframework.stereotype.Component;

@Component
public class OvertimePolicyMapper {

    public OvertimePolicy mapToEntity(OvertimePolicyRequest request) {
        if (request == null) {
            return null;
        }

        return OvertimePolicy.builder()
                .name(request.getName())
                .country(request.getCountry())
                .state(request.getState())
                .standardDailyMinutes(request.getStandardDailyMinutes())
                .standardWeeklyMinutes(request.getStandardWeeklyMinutes())
                .weekdayRule(mapRule(request.getWeekdayRule()))
                .weekendRule(mapRule(request.getWeekendRule()))
                .holidayRule(mapRule(request.getHolidayRule()))
                .build();
    }

    public void updateEntity(OvertimePolicy policy, OvertimePolicyRequest request) {
        if (policy == null || request == null) {
            return;
        }

        policy.setName(request.getName());
        policy.setCountry(request.getCountry());
        policy.setState(request.getState());
        policy.setStandardDailyMinutes(request.getStandardDailyMinutes());
        policy.setStandardWeeklyMinutes(request.getStandardWeeklyMinutes());
        policy.setWeekdayRule(mapRule(request.getWeekdayRule()));
        policy.setWeekendRule(mapRule(request.getWeekendRule()));
        policy.setHolidayRule(mapRule(request.getHolidayRule()));
    }

    public OvertimePolicyResponse mapToResponse(OvertimePolicy policy) {
        if (policy == null) {
            return null;
        }

        return OvertimePolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .name(policy.getName())
                .country(policy.getCountry())
                .state(policy.getState())
                .standardDailyMinutes(policy.getStandardDailyMinutes())
                .standardWeeklyMinutes(policy.getStandardWeeklyMinutes())
                .weekdayRule(mapRuleResponse(policy.getWeekdayRule()))
                .weekendRule(mapRuleResponse(policy.getWeekendRule()))
                .holidayRule(mapRuleResponse(policy.getHolidayRule()))
                .build();
    }

    private DayTypeRule mapRule(DayTypeRuleRequest request) {
        if (request == null) {
            return null;
        }

        DayTypeRule rule = new DayTypeRule();
        rule.setMode(request.getMode());
        rule.setMinMinutesToQualify(request.getMinMinutesToQualify());
        rule.setCashMultiplier(request.getCashMultiplier());
        rule.setCompOffMultiplier(request.getCompOffMultiplier());
        rule.setMaxCompOffMinutesBeforeSplit(request.getMaxCompOffMinutesBeforeSplit());
        return rule;
    }

    private DayTypeRuleResponse mapRuleResponse(DayTypeRule rule) {
        if (rule == null) {
            return null;
        }

        return DayTypeRuleResponse.builder()
                .mode(rule.getMode())
                .minMinutesToQualify(rule.getMinMinutesToQualify())
                .cashMultiplier(rule.getCashMultiplier())
                .compOffMultiplier(rule.getCompOffMultiplier())
                .maxCompOffMinutesBeforeSplit(rule.getMaxCompOffMinutesBeforeSplit())
                .build();
    }
}
