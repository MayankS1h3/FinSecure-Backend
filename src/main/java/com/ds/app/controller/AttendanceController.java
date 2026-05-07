package com.ds.app.controller;

import com.ds.app.dto.response.AttendanceResponse;
import com.ds.app.dto.response.MonthlyAttendanceReport;
import com.ds.app.dto.response.TeamAttendanceReportRow;
import com.ds.app.enums.AttendanceStatus;
import com.ds.app.service.IAttendanceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Validated
public class AttendanceController {

    private final IAttendanceService attendanceService;

    // Employee Endpoints
    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @PostMapping("/punch-in")
    public ResponseEntity<AttendanceResponse> punchIn() {
        AttendanceResponse response = attendanceService.punchIn();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @PostMapping("/punch-out")
    public ResponseEntity<AttendanceResponse> punchOut() {
        AttendanceResponse response = attendanceService.punchOut();
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam Integer year
    ) {
        List<AttendanceResponse> response = attendanceService.getMyAttendance(month, year);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping("/date/{date}")
    public ResponseEntity<AttendanceResponse> getMyAttendanceByDate(
            @PathVariable LocalDate date
    ) {
        AttendanceResponse response = attendanceService.getMyAttendanceByDate(date);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping("/report/monthly/me")
    public ResponseEntity<MonthlyAttendanceReport> getMyMonthlyAttendanceReport(
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : Year.now().getValue();
        MonthlyAttendanceReport response =
                attendanceService.getMyMonthlyAttendanceReport(month, targetYear);
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/report/monthly/{employeeId}")
    public ResponseEntity<MonthlyAttendanceReport> getEmployeeMonthlyAttendanceReport(
            @PathVariable Long employeeId,
            @RequestParam @Min(1) @Max(12) Integer month,
            @RequestParam(required = false) Integer year
    ) {
        int targetYear = (year != null) ? year : Year.now().getValue();
        MonthlyAttendanceReport response =
                attendanceService.getEmployeeMonthlyAttendanceReport(employeeId, month, targetYear);
        return ResponseEntity.ok(response);
    }

    // MANAGER Endpoints
    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<AttendanceResponse>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) AttendanceStatus status,
            @PageableDefault(size = 10, page = 0, sort = "date", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<AttendanceResponse> pageResponse =
                attendanceService.getEmployeeAttendance(employeeId, month, year, status, pageable);
        return ResponseEntity.ok(pageResponse);
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/all")
    public ResponseEntity<Page<AttendanceResponse>> getAllAttendanceByDate(
            @RequestParam LocalDate date,
            @PageableDefault(size = 10, page = 0, sort = "attendanceId", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<AttendanceResponse> pageResponse = attendanceService.getAllAttendanceByDate(date, pageable);
        return ResponseEntity.ok(pageResponse);
    }

    @PreAuthorize("hasAuthority('MANAGER')")
    @GetMapping("/report/team/daily")
    public ResponseEntity<List<TeamAttendanceReportRow>> getTeamDailyAttendanceReport(
            @RequestParam LocalDate date
    ) {
        List<TeamAttendanceReportRow> response = attendanceService.getTeamAttendanceReport(date);
        return ResponseEntity.ok(response);
    }
}