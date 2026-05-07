package com.ds.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TimesheetEntryResponse {
    private Long timesheetEntryId;
//    private Long timesheetId;
    private Long employeeId;
    private LocalDate date;
    private String taskDescription;
    private Integer totalMinutesWorked;
    private String formattedTime;
    private Long projectId;
    private String projectName;
}