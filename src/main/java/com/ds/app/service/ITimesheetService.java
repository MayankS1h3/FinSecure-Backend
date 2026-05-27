package com.ds.app.service;

import com.ds.app.dto.request.ApprovalRequest;
import com.ds.app.dto.request.TimesheetApproveRequest;
import com.ds.app.dto.request.TimesheetSubmitRequest;
import com.ds.app.dto.response.AttendanceTimesheetDiscrepancyReport;
import com.ds.app.dto.response.ProjectHoursReportResponse;
import com.ds.app.dto.response.TimesheetResponse;

import com.ds.app.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface ITimesheetService {

    // ==========================================
    // 3. THE APPROVE API (UPDATED)
    // ==========================================
    @Transactional
    TimesheetResponse reviewTimesheet(Long timesheetId, TimesheetApproveRequest request, ApprovalStatus status);

    TimesheetResponse getMyMonthlyTimesheet(Integer month, Integer year);

    // ==========================================
    // 1. THE PREVIEW API (NEW)
    // ==========================================
    com.ds.app.dto.timesheet.TimesheetPreviewResponse previewOvertime(Long timesheetId);

    TimesheetResponse submitMyTimesheet(Long timesheetId, TimesheetSubmitRequest request);

    Page<TimesheetResponse> getPendingTimesheetsForManager(Pageable pageable);

    Page<TimesheetResponse> getTeamTimesheetsByMonthYear(Integer month, Integer year, Pageable pageable);

//    TimesheetResponse reviewTimesheet(Long timesheetId, ApprovalRequest request);

    AttendanceTimesheetDiscrepancyReport getAttendanceTimesheetDiscrepancyReport(Long employeeId, Integer month, Integer year);

	ProjectHoursReportResponse getProjectReportByMonthAndYear(Integer month, Integer year, Long projectId);
}