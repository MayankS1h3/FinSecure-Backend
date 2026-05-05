package com.ds.app;

import com.ds.app.entity.*;
import com.ds.app.enums.*;
import com.ds.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IEmployeeRepository employeeRepo;
    private final IAttendanceRepository attendanceRepo;
    private final ILeaveBalanceRepository leaveBalanceRepo;
    private final ILeaveRepository leaveRepo;
    private final IRegularizationRequestRepository regularizationRepo;
    private final IHolidayRepository holidayRepo;
    private final ITimesheetRepository timesheetRepo;
    private final ITimesheetEntryRepository timesheetEntryRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (employeeRepo.count() > 0) {
            log.info("Database already populated. Skipping demo data seeder.");
            return;
        }

        log.info("Starting demo data generation (including April Discrepancy Scenarios)...");
        String defaultPass = passwordEncoder.encode("123");

        seedHolidays();

        Employee hr = createEmployee("Shruti", "Mehra", "shruti@gmail.com", "shruti_hr", defaultPass, UserRole.HR, null);

        Employee mgr1 = createEmployee("Manish", "Sharma", "manish@gmail.com", "manish_mngr", defaultPass, UserRole.MANAGER, hr);
        Employee mgr2 = createEmployee("Dev", "Kumar", "dev@gmail.com", "dev_mngr", defaultPass, UserRole.MANAGER, hr);

        Employee emp1 = createEmployee("Mayank", "Sharma", "mayank@gmail.com", "mayank", defaultPass, UserRole.EMPLOYEE, mgr1);
        Employee emp2 = createEmployee("Harsh", "Sharma", "harsh@gmail.com", "harsh", defaultPass, UserRole.EMPLOYEE, mgr1);
        Employee emp3 = createEmployee("Viresh", "Raghav", "viresh@gmail.com", "viresh", defaultPass, UserRole.EMPLOYEE, mgr2);
        Employee emp4 = createEmployee("Tanya", "Singh", "tanya@gmail.com", "tanya", defaultPass, UserRole.EMPLOYEE, mgr2);

        List<Employee> allStaff = List.of(hr, mgr1, mgr2, emp1, emp2, emp3, emp4);

        Map<Long, LeaveBalance> employeeBalances = initializeLeaveBalances(allStaff, LocalDate.now().getYear());

        seedDailyRecords(allStaff, employeeBalances, LocalDate.of(2026, 1, 1));
        seedFuturePendingLeaves(List.of(emp1, emp3));

        leaveBalanceRepo.saveAll(employeeBalances.values());

        log.info("Demo data seeding complete! Test the discrepancy report for Month 4.");
    }

    private void seedHolidays() {
        List<Holiday> holidays = List.of(
                Holiday.builder().date(LocalDate.of(2026, 1, 26)).name("Republic Day").type(HolidayType.NATIONAL).build(),
                Holiday.builder().date(LocalDate.of(2026, 3, 3)).name("Holi").type(HolidayType.NATIONAL).build(),
                Holiday.builder().date(LocalDate.of(2026, 3, 30)).name("Company Foundation Day").type(HolidayType.RELIGIOUS).build()
        );
        holidayRepo.saveAll(holidays);
    }

    private Employee createEmployee(String first, String last, String email, String username, String pass, UserRole role, Employee manager) {
        Employee emp = new Employee();
        emp.setUsername(username);
        emp.setPassword(pass);
        emp.setRole(role);
        emp.setFailedLoginAttemptsCount(0);
        emp.setIsAccountLocked(false);
        emp.setFirstName(first);
        emp.setLastName(last);
        emp.setEmail(email);
        emp.setManager(manager);
        return employeeRepo.save(emp);
    }

    private Map<Long, LeaveBalance> initializeLeaveBalances(List<Employee> employees, int year) {
        Map<Long, LeaveBalance> map = new HashMap<>();
        for (Employee emp : employees) {
            LeaveBalance balance = LeaveBalance.builder()
                    .employee(emp).year(year).casualLeaveBalance(8).sickLeaveBalance(10)
                    .earnedLeaveBalance(BigDecimal.valueOf(12.5)).casualLeavesConsumed(0).sickLeavesConsumed(0).build();
            map.put(emp.getUserId(), balance);
        }
        return map;
    }

    private void seedDailyRecords(List<Employee> employees, Map<Long, LeaveBalance> balancesMap, LocalDate startDate) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        Random random = new Random();

        List<Attendance> attendanceBatch = new ArrayList<>();
        List<Leave> leaveBatch = new ArrayList<>();
        List<RegularizationRequest> regBatch = new ArrayList<>();
        List<TimesheetEntry> entryBatch = new ArrayList<>();
        Map<String, Timesheet> timesheetMap = new HashMap<>();

        List<LocalDate> holidayDates = holidayRepo.findAll().stream().map(Holiday::getDate).toList();
        LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate currentWeekEnd = LocalDate.now().with(DayOfWeek.SUNDAY);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY || holidayDates.contains(date)) {
                continue;
            }

            for (Employee emp : employees) {

                // =========================================================
                // LEAVE CURRENT WEEK TIMESHEET ENTRIES EMPTY FOR TESTING
                // =========================================================
                boolean isCurrentWeek = !date.isBefore(currentWeekStart) && !date.isAfter(currentWeekEnd);

                // =========================================================
                // INJECT DISCREPANCIES SPECIFICALLY FOR APRIL 2026
                // =========================================================
                if (date.getMonthValue() == LocalDate.now().getMonthValue()
                        && date.getYear() == LocalDate.now().getYear()) {
                    int day = date.getDayOfMonth();
                    AttendanceStatus status;
                    int attMins = 0;
                    int tsMins = 0;

                    if (day % 3 == 1) {
                        // SCENARIO 1: MISMATCH (Absent in Attendance, but logged 8 hours)
                        status = AttendanceStatus.ABSENT;
                        attMins = 0;
                        tsMins = 480;
                    } else if (day % 3 == 2) {
                        // SCENARIO 2: WARNING (Attended 9 hours, but logged only 5 hours)
                        status = AttendanceStatus.PRESENT;
                        attMins = 540;
                        tsMins = 300;
                    } else {
                        // SCENARIO 3: OK (Attended 8 hours, logged 8 hours)
                        status = AttendanceStatus.PRESENT;
                        attMins = 480;
                        tsMins = 480;
                    }

                    // Save Attendance
                    attendanceBatch.add(createAttendance(emp, date, status,
                            status == AttendanceStatus.PRESENT ? LocalTime.of(9,0) : null,
                            status == AttendanceStatus.PRESENT ? LocalTime.of(18,0) : null,
                            attMins, false));

                    // Save Timesheet Entry
                    if (tsMins > 0 && !isCurrentWeek) {
                        Timesheet ts = getOrCreateTimesheet(emp, date, timesheetMap);
                        TimesheetEntry entry = TimesheetEntry.builder()
                                .employee(emp)
                                .date(date)
                                .taskDescription("April Testing")
                                .totalMinutesWorked(tsMins)
                                .projectId(101L)
                                .projectName("HRMS Core")
                                .build();
                        entryBatch.add(entry);
                        ts.setTotalMonthlyMinutes(ts.getTotalMonthlyMinutes() + tsMins);
                    }
                    continue; // Skip the rest of the historical loop logic for April dates
                }

                // =========================================================
                // NORMAL HISTORY LOGIC (JAN, FEB, MAR) - PERFECT MATCHES
                // =========================================================
                int chance = random.nextInt(100);
                int minsWorked = 0;
                boolean isPresent = false;

                if (chance < 80) {
                    minsWorked = 540;
                    isPresent = true;
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.PRESENT, LocalTime.of(9, random.nextInt(15)), LocalTime.of(18, random.nextInt(30)), minsWorked, false));
                } else if (chance < 85) {
                    minsWorked = 450;
                    isPresent = true;
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.PRESENT, LocalTime.of(10, 30), LocalTime.of(18, 0), minsWorked, true));
                } else if (chance < 90) {
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.MISS_SWIPE, LocalTime.of(9, 0), null, 0, false));
                    regBatch.add(RegularizationRequest.builder().employee(emp).date(date).reason("Forgot to punch out").punchInTime(LocalTime.of(9, 0)).punchOutTime(LocalTime.of(18, 0)).status(RegularizationRequestStatus.PENDING).build());
                } else {
                    LeaveType type = random.nextBoolean() ? LeaveType.SICK : LeaveType.CASUAL;
                    int leaveStateChance = random.nextInt(100);
                    LeaveStatus leaveStatus;
                    boolean managerActed = false;
                    String rejectionReason = null;

                    if (leaveStateChance < 40) { leaveStatus = LeaveStatus.APPROVED; managerActed = true; }
                    else if (leaveStateChance < 55) { leaveStatus = LeaveStatus.REJECTED; managerActed = true; rejectionReason = "Not enough coverage."; }
                    else if (leaveStateChance < 70) { leaveStatus = LeaveStatus.EXPIRED; }
                    else if (leaveStateChance < 80) { leaveStatus = LeaveStatus.WITHDRAWN; }
                    else if (leaveStateChance < 90) { leaveStatus = LeaveStatus.CANCELLED; managerActed = true; }
                    else { leaveStatus = LeaveStatus.CANCELLATION_PENDING; managerActed = true; }

                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.ABSENT, null, null, 0, false));

                    leaveBatch.add(Leave.builder()
                            .employee(emp).startDate(date).endDate(date).totalDays(1)
                            .reasonForLeave(type == LeaveType.SICK ? "Fever" : "Personal Errand")
                            .leaveType(type).status(leaveStatus)
                            .approvedBy(managerActed ? emp.getManager() : null)
                            .approvalDate(managerActed ? date.minusDays(1) : null)
                            .rejectionReason(rejectionReason)
                            .build());

                    if (leaveStatus == LeaveStatus.APPROVED || leaveStatus == LeaveStatus.CANCELLATION_PENDING) {
                        LeaveBalance b = balancesMap.get(emp.getUserId());
                        if (type == LeaveType.SICK) {
                            b.setSickLeaveBalance(b.getSickLeaveBalance() - 1);
                            b.setSickLeavesConsumed(b.getSickLeavesConsumed() + 1);
                        } else {
                            b.setCasualLeaveBalance(b.getCasualLeaveBalance() - 1);
                            b.setCasualLeavesConsumed(b.getCasualLeavesConsumed() + 1);
                        }
                    }
                }

                if (isPresent && !isCurrentWeek) {
                    Timesheet ts = getOrCreateTimesheet(emp, date, timesheetMap);
                    TimesheetEntry entry = TimesheetEntry.builder()
                            .employee(emp)
                            .date(date)
                            .taskDescription("Routine tasks")
                            .totalMinutesWorked(minsWorked)
                            .projectId(101L)
                            .projectName("HRMS Core")
                            .build();

                    entryBatch.add(entry);
                    ts.setTotalMonthlyMinutes(ts.getTotalMonthlyMinutes() + minsWorked);
                }
            }
        }

        processAndApproveTimesheets(timesheetMap);

        attendanceRepo.saveAll(attendanceBatch);
        leaveRepo.saveAll(leaveBatch);
        regularizationRepo.saveAll(regBatch);
        timesheetEntryRepo.saveAll(entryBatch);
        timesheetRepo.saveAll(timesheetMap.values());
    }

    private Timesheet getOrCreateTimesheet(Employee emp, LocalDate date, Map<String, Timesheet> timesheetMap) {
        String key = emp.getUserId() + "-" + date.getYear() + "-" + date.getMonthValue();
        if (!timesheetMap.containsKey(key)) {
            Timesheet ts = Timesheet.builder()
                    .employee(emp).year(date.getYear()).month(date.getMonthValue())
                    .status(TimesheetStatus.DRAFT).totalMonthlyMinutes(0).build();
            timesheetMap.put(key, ts);
        }
        return timesheetMap.get(key);
    }

    private void processAndApproveTimesheets(Map<String, Timesheet> timesheetMap) {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        for (Timesheet ts : timesheetMap.values()) {
            LocalDate tsDate = LocalDate.of(ts.getYear(), ts.getMonth(), 1);
            if (tsDate.isBefore(currentMonthStart)) {
                ts.setStatus(TimesheetStatus.APPROVED);
                ts.setSubmittedAt(tsDate.plusMonths(1).minusDays(1).atTime(18, 0));
                ts.setApprovedBy(ts.getEmployee().getManager());
                ts.setApprovalDate(tsDate.plusMonths(1));
            }
        }
    }

    private void seedFuturePendingLeaves(List<Employee> employeesToRequestLeave) {
        LocalDate nextWeek = LocalDate.now().plusDays(5);
        List<Leave> futureLeaves = new ArrayList<>();
        for (Employee emp : employeesToRequestLeave) {
            futureLeaves.add(Leave.builder()
                    .employee(emp).startDate(nextWeek).endDate(nextWeek).totalDays(1)
                    .reasonForLeave("Doctor appointment").leaveType(LeaveType.SICK)
                    .status(LeaveStatus.PENDING).build());
        }
        leaveRepo.saveAll(futureLeaves);
    }

    private Attendance createAttendance(Employee emp, LocalDate date, AttendanceStatus status, LocalTime in, LocalTime out, int mins, boolean isLate) {
        return Attendance.builder().employee(emp).date(date).status(status).punchInTime(in).punchOutTime(out).totalMinutesWorked(mins).isLate(isLate).isRegularized(false).build();
    }
}