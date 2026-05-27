package com.ds.app.dto.response;

import com.ds.app.enums.CompensationMode;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ManagerReviewResponse {
    private Long timesheetId;
    private String employeeName;
    private String status;
    private Integer totalOvertimeMinutes;
    private List<ManagerReviewDayDTO> overtimeDays;

    @Data
    @Builder
    public static class ManagerReviewDayDTO {
        private LocalDate date;
        private Integer extraMinutes;

        // If the system handled it automatically (e.g., POLICY_SPLIT)
        private CompensationMode systemAction;

        // If the employee chose it (e.g., COMP_OFF_ONLY)
        private CompensationMode employeeRequested;
    }
}