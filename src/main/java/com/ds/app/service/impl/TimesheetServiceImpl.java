package com.ds.app.service.impl;

import com.ds.app.dto.request.ApprovalRequest;
import com.ds.app.dto.request.TimesheetApproveRequest;
import com.ds.app.dto.request.TimesheetSubmitRequest;
import com.ds.app.dto.response.*;
import com.ds.app.dto.response.OvertimeResponse;
import com.ds.app.dto.timesheet.TimesheetPreviewResponse;
import com.ds.app.entity.*;
import com.ds.app.enums.ApprovalStatus;
import com.ds.app.enums.AttendanceStatus;
import com.ds.app.enums.CompensationMode;
import com.ds.app.enums.TimesheetStatus;
import com.ds.app.exception.ForbiddenException;
import com.ds.app.exception.InvalidTimesheetStateException;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.TimesheetMapper;
import com.ds.app.repository.*;
import com.ds.app.service.IEmailService;
import com.ds.app.service.ILeaveBalanceService;
import com.ds.app.service.IOvertimeCalculationService;
import com.ds.app.service.ITimesheetService;
import com.ds.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetServiceImpl implements ITimesheetService {

    // Existing Repositories
    private final ITimesheetRepository timesheetRepository;
    private final IAttendanceRepository attendanceRepository;
    private final IEmployeeRepository employeeRepository;
    private final ITimesheetEntryRepository timesheetEntryRepository;

    // New Repositories & Services for Enterprise Overtime Math
    private final IOvertimePolicyRepository overtimePolicyRepository;
    private final IHolidayRepository holidayRepository;
    private final IOvertimeCalculationService overtimeCalcService;
    private final ILeaveBalanceService leaveBalanceService;

    // Utilities
    private final TimesheetMapper timesheetMapper;
    private final SecurityUtils securityUtils;
    private final IEmailService emailService;

    // ==========================================
    // 1. THE PREVIEW API
    // ==========================================
    @Override
    public TimesheetPreviewResponse previewOvertime(Long timesheetId) {
        Employee me = securityUtils.getLoggedInEmployee();

        Timesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found"));

        if (!timesheet.getEmployee().getUserId().equals(me.getUserId())) {
            throw new ForbiddenException("Cannot preview someone else's timesheet");
        }

        OvertimePolicy policy = fetchEmployeePolicy(me);
        List<TimesheetEntry> entries = getEntriesForTimesheet(me.getUserId(), timesheet.getMonth(), timesheet.getYear());

        int totalProjectedOvertime = 0;
        List<TimesheetPreviewResponse.OvertimeDayPreview> breakdown = new ArrayList<>();

        for (TimesheetEntry entry : entries) {
            boolean isHoliday = isHoliday(entry.getDate());
            OvertimeResponse calc = overtimeCalcService.calculateDailyOvertime(entry, policy, isHoliday);

            if (calc.getExtraMinutes() > 0) {
                totalProjectedOvertime += calc.getExtraMinutes();

                TimesheetPreviewResponse.OvertimeDayPreview dayPreview = TimesheetPreviewResponse.OvertimeDayPreview.builder()
                        .date(entry.getDate())
                        .dayType(isHoliday ? "HOLIDAY" : (entry.getDate().getDayOfWeek().getValue() >= 6 ? "WEEKEND" : "WEEKDAY"))
                        .extraMinutes(calc.getExtraMinutes())
                        .mode(calc.getAppliedMode())
                        .message(calc.getMessage())
                        .requiresEmployeeChoice(calc.isRequiresEmployeeChoice())
                        .allowedOptions(calc.isRequiresEmployeeChoice() ? List.of(CompensationMode.CASH_ONLY, CompensationMode.COMP_OFF_ONLY) : null)
                        .build();

                breakdown.add(dayPreview);
            }
        }

        return TimesheetPreviewResponse.builder()
                .timesheetId(timesheetId)
                .projectedTotalOvertimeMinutes(totalProjectedOvertime)
                .previewBreakdown(breakdown)
                .build();
    }

    // ==========================================
    // 2. THE SUBMIT API
    // ==========================================
    @Override
    @Transactional
    public TimesheetResponse submitMyTimesheet(Long timesheetId, TimesheetSubmitRequest request) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        Timesheet timesheet = timesheetRepository.findByTimesheetIdAndEmployeeUserId(timesheetId, loggedInEmployee.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT) {
            throw new InvalidTimesheetStateException("Only DRAFT timesheet can be submitted");
        }
        if (timesheet.getTotalMonthlyMinutes() == null || timesheet.getTotalMonthlyMinutes() <= 0) {
            throw new InvalidTimesheetStateException("Cannot submit an empty timesheet.");
        }

        // Apply employee choices to the specific TimesheetEntries
        if (request != null && request.getEmployeeChoices() != null) {
            for (TimesheetSubmitRequest.EmployeeChoiceDTO choice : request.getEmployeeChoices()) {
                TimesheetEntry entry = timesheetEntryRepository.findByEmployee_UserIdAndDate(loggedInEmployee.getUserId(), choice.getDate())
                        .orElseThrow(() -> new ResourceNotFoundException("Entry not found for date: " + choice.getDate()));

                entry.setEmployeePreference(choice.getPreference());
                entry.setManagerOverride(null); // Ensure clean state
                timesheetEntryRepository.save(entry);
            }
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());
        timesheet.setApprovedBy(null);
        timesheet.setApprovalDate(null);
        timesheet.setRejectionReason(null);

        emailService.notifyManagerForTimesheetSubmission(loggedInEmployee, timesheet);

        return timesheetMapper.mapToResponse(timesheet);
    }

    // ==========================================
    // 3. THE APPROVE API
    // ==========================================
    @Override
    @Transactional
    public TimesheetResponse reviewTimesheet(Long timesheetId, TimesheetApproveRequest request, ApprovalStatus status) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();

        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

        if (ts.getEmployee().getManager() == null || !ts.getEmployee().getManager().getUserId().equals(loggedInManager.getUserId())) {
            throw new ForbiddenException("You are not authorised to review this timesheet");
        }
        if (ts.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new InvalidTimesheetStateException("Only SUBMITTED timesheet can be reviewed");
        }

        ts.setApprovedBy(loggedInManager);
        ts.setApprovalDate(LocalDate.now());

        if (status == ApprovalStatus.REJECTED) {
            ts.setStatus(TimesheetStatus.REJECTED);
            // Assuming your TimesheetApproveRequest has a getRejectionReason() or it's passed separately
            ts.setRejectionReason(request != null ? request.getRejectionReason() : "Rejected by manager");
        } else {
            // --- IT IS APPROVED! RUN THE TIME VALUATION ENGINE ---
            ts.setStatus(TimesheetStatus.APPROVED);
            ts.setRejectionReason(null);

            // 1. Apply Manager Overrides to Entries
            if (request != null && request.getManagerOverrides() != null) {
                for (TimesheetApproveRequest.ManagerOverrideDTO override : request.getManagerOverrides()) {
                    TimesheetEntry entry = timesheetEntryRepository.findByEmployee_UserIdAndDate(ts.getEmployee().getUserId(), override.getDate())
                            .orElseThrow(() -> new ResourceNotFoundException("Entry not found for date: " + override.getDate()));
                    entry.setManagerOverride(override.getFinalDecision());
                    timesheetEntryRepository.save(entry);
                }
            }

            // 2. Calculate Final Overtime Math
            OvertimePolicy policy = fetchEmployeePolicy(ts.getEmployee());
            List<TimesheetEntry> entries = getEntriesForTimesheet(ts.getEmployee().getUserId(), ts.getMonth(), ts.getYear());

            int finalTotalOt = 0;
            int finalCash = 0;
            int finalCompOff = 0;

            for (TimesheetEntry entry : entries) {
                OvertimeResponse calc = overtimeCalcService.calculateDailyOvertime(entry, policy, isHoliday(entry.getDate()));
                finalTotalOt += calc.getExtraMinutes();
                finalCash += calc.getCashMinutes();
                finalCompOff += calc.getCompOffMinutes();
            }

            // 3. Save Totals to Database
            ts.setOvertimeMinutes(finalTotalOt);
            ts.setOvertimeCashMinutes(finalCash);
            ts.setCompOffEarnedMinutes(finalCompOff);

            // 4. Deposit Comp-Off to Leave Bank!
            if (finalCompOff > 0) {
                leaveBalanceService.depositCompOff(ts.getEmployee().getUserId(), ts.getYear(), finalCompOff);
            }
        }

        emailService.notifyEmployeeForTimesheetDecision(ts.getEmployee(), ts);
        return timesheetMapper.mapToResponse(ts);
    }

    // ==========================================
    // 4. HELPER METHODS
    // ==========================================

    private OvertimePolicy fetchEmployeePolicy(Employee employee) {
        if (employee.getOvertimePolicyId() == null) {
            throw new InvalidTimesheetStateException("Employee does not have an assigned Overtime Policy.");
        }
        return overtimePolicyRepository.findById(employee.getOvertimePolicyId())
                .orElseThrow(() -> new ResourceNotFoundException("Assigned Overtime Policy not found in the database."));
    }

    private List<TimesheetEntry> getEntriesForTimesheet(Long employeeId, int month, int year) {
        return timesheetEntryRepository.findByEmployee_UserIdAndMonthAndYear(employeeId, month, year, Pageable.unpaged()).getContent();
    }

    private boolean isHoliday(LocalDate date) {
        // Calls your existing holiday repository to check if the date is a registered holiday
        return holidayRepository.existsByDate(date);
    }

    // ==========================================
    // 5. EXISTING FETCH & REPORTING METHODS
    // ==========================================

    @Override
    public TimesheetResponse getMyMonthlyTimesheet(Integer month, Integer year) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();
        Timesheet existingTimesheet = timesheetRepository.findByEmployee_UserIdAndMonthAndYear(loggedInEmployee.getUserId(), month, year)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found for month/year"));
        return timesheetMapper.mapToResponse(existingTimesheet);
    }

    @Override
    public Page<TimesheetResponse> getPendingTimesheetsForManager(Pageable pageable) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();
        return timesheetRepository
                .findByEmployee_Manager_UserIdAndStatus(loggedInManager.getUserId(), TimesheetStatus.SUBMITTED, pageable)
                .map(timesheetMapper::mapToResponse);
    }

    @Override
    public Page<TimesheetResponse> getTeamTimesheetsByMonthYear(Integer month, Integer year, Pageable pageable) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();
        return timesheetRepository
                .findByEmployee_Manager_UserIdAndMonthAndYear(loggedInManager.getUserId(), month, year, pageable)
                .map(timesheetMapper::mapToResponse);
    }

    @Override
    @Transactional
    public AttendanceTimesheetDiscrepancyReport getAttendanceTimesheetDiscrepancyReport(Long employeeId, Integer month, Integer year) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id: " + employeeId));

        if (employee.getManager() == null ||
                !employee.getManager().getUserId().equals(loggedInManager.getUserId())) {
            throw new ForbiddenException("You are not allowed to view this report");
        }

        List<Attendance> attendanceList =
                attendanceRepository.findAttendanceByEmployeeUserIdAndMonthAndYear(employeeId, month, year);

        Map<LocalDate, Attendance> attendanceByDate = attendanceList.stream()
                .collect(Collectors.toMap(Attendance::getDate, a -> a, (a1, a2) -> a1));

        Map<LocalDate, Double> timesheetHoursByDate = timesheetEntryRepository.findByEmployee_UserIdAndMonthAndYear(
                        employeeId, month, year, Pageable.unpaged()).getContent().stream()
                .collect(Collectors.groupingBy(
                        TimesheetEntry::getDate,
                        Collectors.summingDouble(e -> e.getTotalMinutesWorked() / 60.0)
                ));

        Set<LocalDate> allDates = new TreeSet<>();
        allDates.addAll(attendanceByDate.keySet());
        allDates.addAll(timesheetHoursByDate.keySet());

        List<AttendanceTimesheetDiscrepancyRow> rows = allDates.stream()
                .map(date -> {
                    Attendance a = attendanceByDate.get(date);

                    AttendanceStatus status = (a != null && a.getStatus() != null)
                            ? a.getStatus()
                            : AttendanceStatus.ABSENT;

                    double attendanceHours = (a != null && a.getTotalMinutesWorked() != null)
                            ? round2(a.getTotalMinutesWorked() / 60.0)
                            : 0.0;

                    double timesheetHours = round2(timesheetHoursByDate.getOrDefault(date, 0.0));

                    String discrepancy = evaluate(status, attendanceHours, timesheetHours);

                    return AttendanceTimesheetDiscrepancyRow.builder()
                            .date(date)
                            .attendanceStatus(status)
                            .attendanceHours(attendanceHours)
                            .timesheetHours(timesheetHours)
                            .discrepancy(discrepancy)
                            .build();
                })
                .toList();

        return AttendanceTimesheetDiscrepancyReport.builder()
                .employeeId(employee.getUserId())
                .employeeName(employee.getFirstName() + " " + employee.getLastName())
                .month(month)
                .year(year)
                .rows(rows)
                .build();
    }

    @Override
    public ProjectHoursReportResponse getProjectReportByMonthAndYear(Integer month, Integer year, Long projectId) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();

        List<EmployeeProjectHoursRow> employeeWiseRows = timesheetEntryRepository.findEmployeeWiseProjectHoursForManager(month, year, projectId, loggedInManager.getUserId());
        double totalProjectHours = employeeWiseRows.stream()
                .mapToDouble(row -> row.getHoursWorked())
                .sum();

        return ProjectHoursReportResponse.builder()
                .projectId(projectId)
                .totalProjectHours(totalProjectHours)
                .employeeWiseRows(employeeWiseRows)
                .build();
    }

    private String evaluate(AttendanceStatus status, double attendanceHours, double timesheetHours) {
        if (status == AttendanceStatus.ABSENT && timesheetHours > 0) return "MISMATCH";
        if (Math.abs(attendanceHours - timesheetHours) > 0.5) return "WARNING";
        return "OK";
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}