package com.ds.app.dto.response;

import com.ds.app.enums.TimesheetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetResponse {
	private Long timesheetId;
	private Long employeeId;
	private String employeeName;
	private Integer month;
	private Integer year;
	private TimesheetStatus status;
	private LocalDateTime submittedAt;
	private String approvedByName;
	private LocalDate approvalDate;
	private String rejectionReason;
    private Integer totalMonthlyMinutes;
    private String formattedTotalTime;
}
