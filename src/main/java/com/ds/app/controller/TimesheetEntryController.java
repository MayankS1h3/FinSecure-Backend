package com.ds.app.controller;

import com.ds.app.dto.request.TimesheetEntryRequest;
import com.ds.app.dto.request.WeeklyTimesheetEntryRequest;
import com.ds.app.dto.response.TimesheetEntryResponse;
import com.ds.app.dto.response.WeeklyTimesheetEntryResponse;
import com.ds.app.service.ITimesheetEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timesheet-entries")
@RequiredArgsConstructor
public class TimesheetEntryController {

    private final ITimesheetEntryService timesheetEntryService;

    @PreAuthorize("hasAuthority('EMPLOYEE')")
    @PostMapping
    public ResponseEntity<TimesheetEntryResponse> addMyEntry(@Valid @RequestBody TimesheetEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheetEntryService.addMyEntry(request));
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE')")
    @PostMapping("/weekly-entry")
    public ResponseEntity<WeeklyTimesheetEntryResponse> addWeeklyEntry(@Valid @RequestBody WeeklyTimesheetEntryRequest request) {
        WeeklyTimesheetEntryResponse response = timesheetEntryService.addWeeklyEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping
    public ResponseEntity<Page<TimesheetEntryResponse>> getMyEntries(
            @RequestParam Integer month,
            @RequestParam Integer year,
            @PageableDefault(size = 31, page = 0)
            Pageable pageable
    ) {
        return ResponseEntity.ok(timesheetEntryService.getMyEntries(month, year, pageable));
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @GetMapping("/range")
    public ResponseEntity<List<TimesheetEntryResponse>> getMyEntriesByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(timesheetEntryService.getMyEntriesByDateRange(startDate, endDate));
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @PutMapping("/{entryId}")
    public ResponseEntity<TimesheetEntryResponse> updateMyEntry(
            @PathVariable Long entryId,
            @Valid @RequestBody TimesheetEntryRequest request
    ) {
        return ResponseEntity.ok(timesheetEntryService.updateMyEntry(entryId, request));
    }

    @PreAuthorize("hasAnyAuthority('EMPLOYEE','MANAGER')")
    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> deleteMyEntry(@PathVariable Long entryId) {
        timesheetEntryService.deleteMyEntry(entryId);
        return ResponseEntity.noContent().build();
    }
}