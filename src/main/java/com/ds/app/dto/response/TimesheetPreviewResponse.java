package com.ds.app.dto.timesheet;

import com.ds.app.enums.CompensationMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TimesheetPreviewResponse {
    private Long timesheetId;
    private Integer projectedTotalOvertimeMinutes;
    private List<OvertimeDayPreview> previewBreakdown;

    @Data
    @Builder
    public static class OvertimeDayPreview {
        private LocalDate date;
        private String dayType; // e.g., "WEEKEND", "WEEKDAY"
        private Integer extraMinutes;
        private CompensationMode mode; // e.g., POLICY_SPLIT or EMPLOYEE_CHOICE
        private String message;
        private Boolean requiresEmployeeChoice;

        // Populated if requiresEmployeeChoice is true (e.g., [CASH_ONLY, COMP_OFF_ONLY])
        private List<CompensationMode> allowedOptions;
    }
}