package com.ds.app.scheduler;

import com.ds.app.entity.Employee;
import com.ds.app.entity.Leave;
import com.ds.app.entity.LeaveBalance;
import com.ds.app.entity.TimesheetEntry;
import com.ds.app.enums.LeaveStatus;
import com.ds.app.enums.LeaveType;
import com.ds.app.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LeaveBalanceScheduler {

    private final IEmployeeRepository employeeRepository;
    private final ILeaveBalanceRepository leaveBalanceRepository;
    private final ITimesheetEntryRepository timesheetEntryRepository;
    private final ILeaveRepository leaveRepository;
    private final IHolidayRepository holidayRepository;

    @Scheduled(cron = "0 10 0 1 1 ?")
    @Transactional
    public void createYearlyLeaveBalances() {
        int year = Year.now().getValue();

        List<Employee> allEmployees = employeeRepository.findAll();

        allEmployees.stream()
                .filter(emp -> !leaveBalanceRepository.existsByEmployeeUserIdAndYear(emp.getUserId(), year))
                .forEach(emp -> {
                    BigDecimal carryForward = leaveBalanceRepository
                            .findByEmployeeUserIdAndYear(emp.getUserId(), year - 1)
                            .map(prev -> prev.getEarnedLeaveBalance() == null
                                    ? BigDecimal.ZERO
                                    : prev.getEarnedLeaveBalance().max(BigDecimal.ZERO))
                            .orElse(BigDecimal.ZERO);

                    LeaveBalance newBalance = LeaveBalance.builder()
                            .employee(emp)
                            .year(year)
                            .sickLeaveBalance(10)
                            .casualLeaveBalance(8)
                            .earnedLeaveBalance(carryForward)
                            .carriedForwardEarnedDays(carryForward)
                            .build();

                    leaveBalanceRepository.save(newBalance);
                });
    }

    @Scheduled(cron = "0 10 0 1 * ?")
    @Transactional
    public void creditMonthlyEarnedLeaves() {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        int daysInMonth = previousMonth.lengthOfMonth();

        List<Employee> allEmployees = employeeRepository.findAll();

        for (Employee employee : allEmployees) {
            double totalPaidDays = getTotalPaidDays(employee, previousMonth);

            BigDecimal credit = BigDecimal.valueOf(totalPaidDays)
                    .divide(BigDecimal.valueOf(daysInMonth), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(1.5));

            if (credit.compareTo(BigDecimal.valueOf(1.5)) > 0) {
                credit = BigDecimal.valueOf(1.5);
            }

            credit = credit.setScale(2, RoundingMode.HALF_UP);

            int yearToCredit = Year.now().getValue();
            LeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeUserIdAndYear(employee.getUserId(), yearToCredit)
                    .orElseThrow(() -> new IllegalStateException("Leave balance not found for employee: " + employee.getUserId()));

            leaveBalance.setEarnedLeaveBalance(leaveBalance.getEarnedLeaveBalance().add(credit));
        }
    }

    public double getTotalPaidDays(Employee employee, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // 1) Weekoffs + holidays
        Set<LocalDate> paidDays = new HashSet<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                paidDays.add(d);
            }
        }
        paidDays.addAll(holidayRepository.findDatesBetween(start, end));

        List<Leave> approvedLeaves = leaveRepository.findApprovedPaidLeavesOverlappingRange(
                employee.getUserId(),
                LeaveStatus.APPROVED,
                LeaveType.UNPAID,
                start,
                end
        );

        for (Leave leave : approvedLeaves) {
            if (leave.getLeaveType() == LeaveType.UNPAID) {
                continue;
            }
            LocalDate from = leave.getStartDate().isBefore(start) ? start : leave.getStartDate();
            LocalDate to = leave.getEndDate().isAfter(end) ? end : leave.getEndDate();

            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                paidDays.add(d);
            }
        }

        double timesheetPaid = 0.0;
//        Timesheet ts = timesheetRepository
//                .findByEmployeeUserIdAndMonthAndYear(employee.getUserId(), month.getMonthValue(), month.getYear())
//                .orElse(null);
        
        List<TimesheetEntry> entries = timesheetEntryRepository.findByEmployee_UserIdAndMonthAndYear(
        		employee.getUserId(),
        		month.getMonthValue(),
        		month.getYear());

            timesheetPaid = entries.stream()
                    .filter(e -> e.getDate() != null)
                    .filter(e -> !e.getDate().isBefore(start) && !e.getDate().isAfter(end))
                    .filter(e -> e.getTotalMinutesWorked() != null)
                    .mapToDouble(e -> e.getTotalMinutesWorked() >= 240 ? 1.0 : 0.5)
                    .sum();

        double total = paidDays.size() + timesheetPaid;
        return Math.min(total, month.lengthOfMonth());
    }
}