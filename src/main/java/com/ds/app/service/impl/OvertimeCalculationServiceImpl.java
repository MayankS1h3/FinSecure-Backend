package com.ds.app.service.impl;

import com.ds.app.dto.response.OvertimeResponse;
import com.ds.app.entity.DayTypeRule;
import com.ds.app.entity.OvertimePolicy;
import com.ds.app.entity.TimesheetEntry;
import com.ds.app.enums.CompensationMode;
import com.ds.app.service.IOvertimeCalculationService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
public class OvertimeCalculationServiceImpl implements IOvertimeCalculationService {

    @Override
    public OvertimeResponse calculateDailyOvertime(TimesheetEntry entry, OvertimePolicy policy, boolean isHoliday) {

        // 1. Calculate Extra Minutes
        int extraMinutes = entry.getTotalMinutesWorked() - policy.getStandardDailyMinutes();

        if (extraMinutes <= 0) {
            return OvertimeResponse.builder()
                    .extraMinutes(0).cashMinutes(0).compOffMinutes(0)
                    .appliedMode(CompensationMode.NOT_ELIGIBLE)
                    .build();
        }

        // 2. Identify which rule applies to this specific day
        DayTypeRule rule = determineRule(entry.getDate(), policy, isHoliday);

        // 3. Check Minimum Qualification Threshold
        if (rule.getMinMinutesToQualify() != null && extraMinutes < rule.getMinMinutesToQualify()) {
            return OvertimeResponse.builder()
                    .extraMinutes(extraMinutes).cashMinutes(0).compOffMinutes(0)
                    .appliedMode(CompensationMode.NOT_ELIGIBLE)
                    .message("Did not meet minimum extra minutes (" + rule.getMinMinutesToQualify() + " mins) to qualify for overtime.")
                    .build();
        }

        // 4. Determine Effective Mode (Handling Employee Choice & Manager Overrides)
        CompensationMode effectiveMode = rule.getMode();

        if (effectiveMode == CompensationMode.EMPLOYEE_CHOICE) {
            // Manager override wins. If no override, employee preference wins.
            CompensationMode userChoice = (entry.getManagerOverride() != null)
                    ? entry.getManagerOverride()
                    : entry.getEmployeePreference();

            if (userChoice == null) {
                // This happens during the "Preview" phase before the employee has submitted a choice!
                return OvertimeResponse.builder()
                        .extraMinutes(extraMinutes).cashMinutes(0).compOffMinutes(0)
                        .appliedMode(CompensationMode.EMPLOYEE_CHOICE)
                        .requiresEmployeeChoice(true)
                        .message("Please select your preferred compensation method.")
                        .build();
            } else {
                effectiveMode = userChoice;
            }
        }

        // 5. Calculate Final Math based on the Effective Mode
        return executeMath(extraMinutes, rule, effectiveMode);
    }

    private DayTypeRule determineRule(LocalDate date, OvertimePolicy policy, boolean isHoliday) {
        if (isHoliday) {
            return policy.getHolidayRule();
        }

        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return policy.getWeekendRule();
        }

        return policy.getWeekdayRule();
    }

    private OvertimeResponse executeMath(int extraMinutes, DayTypeRule rule, CompensationMode mode) {
        double cashMultiplier = (rule.getCashMultiplier() != null) ? rule.getCashMultiplier() : 1.0;
        double compMultiplier = (rule.getCompOffMultiplier() != null) ? rule.getCompOffMultiplier() : 1.0;

        int cashMins = 0;
        int compMins = 0;

        switch (mode) {
            case NOT_ELIGIBLE:
                break;

            case CASH_ONLY:
                cashMins = (int) (extraMinutes * cashMultiplier);
                break;

            case COMP_OFF_ONLY:
                compMins = (int) (extraMinutes * compMultiplier);
                break;

            case POLICY_SPLIT:
                int maxCompLimit = (rule.getMaxCompOffMinutesBeforeSplit() != null)
                        ? rule.getMaxCompOffMinutesBeforeSplit()
                        : 0;

                if (extraMinutes <= maxCompLimit) {
                    compMins = (int) (extraMinutes * compMultiplier);
                } else {
                    compMins = (int) (maxCompLimit * compMultiplier);
                    cashMins = (int) ((extraMinutes - maxCompLimit) * cashMultiplier);
                }
                break;

            default:
                break;
        }

        return OvertimeResponse.builder()
                .extraMinutes(extraMinutes)
                .cashMinutes(cashMins)
                .compOffMinutes(compMins)
                .appliedMode(mode)
                .requiresEmployeeChoice(false)
                .build();
    }
}