package com.ds.app.mapper;

import com.ds.app.dto.response.TimesheetResponse;
import com.ds.app.entity.Timesheet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimesheetMapper {

    public TimesheetResponse mapToResponse(Timesheet timesheet) {
    	
        String employeeName = timesheet.getEmployee().getFirstName() + " " + timesheet.getEmployee().getLastName();
        String approvedByName = timesheet.getApprovedBy() == null
                ? null
                : timesheet.getApprovedBy().getFirstName() + " " + timesheet.getApprovedBy().getLastName();

        int totalMins = timesheet.getTotalMonthlyMinutes() != null ? timesheet.getTotalMonthlyMinutes() : 0;

        int hours = totalMins / 60;
        int mins = totalMins % 60;

        return TimesheetResponse.builder()
                .timesheetId(timesheet.getTimesheetId())
                .employeeId(timesheet.getEmployee().getUserId())
                .employeeName(employeeName)
                .month(timesheet.getMonth())
                .year(timesheet.getYear())
                .status(timesheet.getStatus())
                .submittedAt(timesheet.getSubmittedAt())
                .approvedByName(approvedByName)
                .approvalDate(timesheet.getApprovalDate())
                .rejectionReason(timesheet.getRejectionReason())
                .totalMonthlyMinutes(totalMins)
                .formattedTotalTime(String.format("%02d:%02d", hours, mins))
                .build();
    }
}