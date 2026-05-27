package com.ds.app.controller;

import com.ds.app.dto.request.TimesheetApproveRequest;
import com.ds.app.dto.request.TimesheetSubmitRequest;
import com.ds.app.dto.response.AttendanceTimesheetDiscrepancyReport;
import com.ds.app.dto.response.ProjectHoursReportResponse;
import com.ds.app.dto.response.TimesheetResponse;
import com.ds.app.dto.timesheet.TimesheetPreviewResponse;
import com.ds.app.enums.ApprovalStatus;
import com.ds.app.service.ITimesheetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/timesheets")
@RequiredArgsConstructor
@Validated
public class TimesheetController {

    private final ITimesheetService timesheetService;

    // ==========================================
    // EMPLOYEE ENDPOINTS
    // ==========================================

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping("/my")
    public ResponseEntity<TimesheetResponse> getMyMonthlyTimesheet(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam Integer year
    ) {
        return ResponseEntity.ok(timesheetService.getMyMonthlyTimesheet(month, year));
    }

    // NEW: Overtime Preview API
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping("/{timesheetId}/preview-overtime")
    public ResponseEntity<TimesheetPreviewResponse> previewOvertime(@PathVariable Long timesheetId) {
        return ResponseEntity.ok(timesheetService.previewOvertime(timesheetId));
    }

    // UPDATED: Now accepts the employee's overtime choices
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @PostMapping("/{timesheetId}/submit")
    public ResponseEntity<TimesheetResponse> submitMyTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody(required = false) TimesheetSubmitRequest request
    ) {
        return ResponseEntity.ok(timesheetService.submitMyTimesheet(timesheetId, request));
    }


    // ==========================================
    // MANAGER ENDPOINTS
    // ==========================================

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/pending")
    public ResponseEntity<Page<TimesheetResponse>> getPendingTimesheetsForManager(
            @PageableDefault(size = 10, page = 0, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(timesheetService.getPendingTimesheetsForManager(pageable));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/team")
    public ResponseEntity<Page<TimesheetResponse>> getTeamTimesheetsByMonthYear(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam Integer year,
            @PageableDefault(size = 10, page = 0, sort = "submittedAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(timesheetService.getTeamTimesheetsByMonthYear(month, year, pageable));
    }

    // UPDATED: Explicit Approve Endpoint (Runs the Overtime Engine)
    @PreAuthorize("hasAuthority('MANAGER')")
    @PostMapping("/{timesheetId}/approve")
    public ResponseEntity<TimesheetResponse> approveTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody(required = false) TimesheetApproveRequest request
    ) {
        return ResponseEntity.ok(timesheetService.reviewTimesheet(timesheetId, request, ApprovalStatus.APPROVED));
    }

    // UPDATED: Explicit Reject Endpoint
    @PreAuthorize("hasAuthority('MANAGER')")
    @PostMapping("/{timesheetId}/reject")
    public ResponseEntity<TimesheetResponse> rejectTimesheet(
            @PathVariable Long timesheetId,
            @Valid @RequestBody(required = false) TimesheetApproveRequest request
    ) {
        return ResponseEntity.ok(timesheetService.reviewTimesheet(timesheetId, request, ApprovalStatus.REJECTED));
    }

    // ==========================================
    // REPORTING ENDPOINTS
    // ==========================================

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/reports/discrepancy/{employeeId}")
    public ResponseEntity<AttendanceTimesheetDiscrepancyReport> getAttendanceTimesheetDiscrepancyReport(
            @PathVariable Long employeeId,
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam Integer year
    ) {
        return ResponseEntity.ok(timesheetService.getAttendanceTimesheetDiscrepancyReport(employeeId, month, year));
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/project-report")
    public ResponseEntity<ProjectHoursReportResponse> getProjectWiseReport(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @RequestParam Long projectId
    ) {
        ProjectHoursReportResponse response = timesheetService.getProjectReportByMonthAndYear(month, year, projectId);
        return ResponseEntity.ok(response);
    }
}