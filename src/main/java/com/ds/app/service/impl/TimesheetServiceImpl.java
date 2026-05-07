package com.ds.app.service.impl;

import com.ds.app.dto.request.ApprovalRequest;
import com.ds.app.dto.response.AttendanceTimesheetDiscrepancyReport;
import com.ds.app.dto.response.AttendanceTimesheetDiscrepancyRow;
import com.ds.app.dto.response.EmployeeProjectHoursRow;
import com.ds.app.dto.response.ProjectHoursReportResponse;
import com.ds.app.dto.response.TimesheetResponse;
import com.ds.app.entity.Attendance;
import com.ds.app.entity.Employee;
import com.ds.app.entity.Timesheet;
import com.ds.app.entity.TimesheetEntry;
import com.ds.app.enums.ApprovalStatus;
import com.ds.app.enums.AttendanceStatus;
import com.ds.app.enums.TimesheetStatus;
import com.ds.app.exception.ForbiddenException;
import com.ds.app.exception.InvalidTimesheetStateException;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.TimesheetMapper;
import com.ds.app.repository.IAttendanceRepository;
import com.ds.app.repository.IEmployeeRepository;
import com.ds.app.repository.ITimesheetEntryRepository;
import com.ds.app.repository.ITimesheetRepository;
import com.ds.app.service.IEmailService;
import com.ds.app.service.ITimesheetService;
import com.ds.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimesheetServiceImpl implements ITimesheetService {

    private final ITimesheetRepository timesheetRepository;
    private final IAttendanceRepository attendanceRepository;
    private final IEmployeeRepository employeeRepository;
    private final TimesheetMapper timesheetMapper;
    private final SecurityUtils securityUtils;
    private final IEmailService emailService;
    private final ITimesheetEntryRepository timesheetEntryRepository;

    @Override
    public TimesheetResponse getMyMonthlyTimesheet(Integer month, Integer year) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        Timesheet existingTimesheet = timesheetRepository.findByEmployee_UserIdAndMonthAndYear(loggedInEmployee.getUserId(), month, year)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found for month/year"));
        
        return timesheetMapper.mapToResponse(existingTimesheet);
    }

    @Override
    @Transactional
    public TimesheetResponse submitMyTimesheet(Long timesheetId) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        Timesheet timesheet = timesheetRepository.findByTimesheetIdAndEmployeeUserId(timesheetId, loggedInEmployee.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

        if (timesheet.getStatus() != TimesheetStatus.DRAFT) {
            throw new InvalidTimesheetStateException("Only DRAFT timesheet can be submitted");
        }

        if (timesheet.getTotalMonthlyMinutes() == null || timesheet.getTotalMonthlyMinutes() <= 0) {
            throw new InvalidTimesheetStateException("Cannot submit an empty timesheet. Please add entries first");
        }

        timesheet.setStatus(TimesheetStatus.SUBMITTED);
        timesheet.setSubmittedAt(LocalDateTime.now());
        timesheet.setApprovedBy(null);
        timesheet.setApprovalDate(null);
        timesheet.setRejectionReason(null);

        emailService.notifyManagerForTimesheetSubmission(loggedInEmployee, timesheet);

        return timesheetMapper.mapToResponse(timesheet);
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
    public TimesheetResponse reviewTimesheet(Long timesheetId, ApprovalRequest request) {
        Employee loggedInManager = securityUtils.getLoggedInEmployee();

        Timesheet ts = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new ResourceNotFoundException("Timesheet not found with id: " + timesheetId));

        if (ts.getEmployee().getManager() == null
                || !ts.getEmployee().getManager().getUserId().equals(loggedInManager.getUserId())) {
            throw new ForbiddenException("You are not authorised to review this timesheet");
        }

        if (ts.getStatus() != TimesheetStatus.SUBMITTED) {
            throw new InvalidTimesheetStateException("Only SUBMITTED timesheet can be reviewed");
        }

        if (request.getStatus() == ApprovalStatus.APPROVED) {
            ts.setStatus(TimesheetStatus.APPROVED);
            ts.setRejectionReason(null);
        } else {
            ts.setStatus(TimesheetStatus.REJECTED);
            ts.setRejectionReason(request.getRejectionReason());
        }

        ts.setApprovedBy(loggedInManager);
        ts.setApprovalDate(LocalDate.now());

        emailService.notifyEmployeeForTimesheetDecision(ts.getEmployee(), ts);

        return timesheetMapper.mapToResponse(ts);
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