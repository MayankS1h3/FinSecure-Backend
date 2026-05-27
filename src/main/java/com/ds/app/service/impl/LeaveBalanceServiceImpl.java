package com.ds.app.service.impl;

import com.ds.app.dto.response.LeaveBalanceResponse;
import com.ds.app.entity.Employee;
import com.ds.app.entity.LeaveBalance;
import com.ds.app.enums.LeaveType;
import com.ds.app.exception.InsufficientLeaveBalanceException;
import com.ds.app.exception.InvalidLeaveStateException;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.LeaveBalanceMapper;
import com.ds.app.repository.IEmployeeRepository;
import com.ds.app.repository.ILeaveBalanceRepository;
import com.ds.app.service.ILeaveBalanceService;
import com.ds.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveBalanceServiceImpl implements ILeaveBalanceService {

    private final ILeaveBalanceRepository leaveBalanceRepository;
    private final IEmployeeRepository employeeRepository;
    private final LeaveBalanceMapper leaveBalanceMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public void reserveLeaves(Long userId, int year, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;

        LeaveBalance lb = findByEmployeeAndYear(userId, year);

        switch (type) {
            case SICK -> {
                int available = lb.getSickLeaveBalance() - lb.getReservedSickLeaves();
                if (available < days)
                    throw new InsufficientLeaveBalanceException("Insufficient sick leave balance");
                lb.setReservedSickLeaves(lb.getReservedSickLeaves() + days);
            }
            case CASUAL -> {
                int available = lb.getCasualLeaveBalance() - lb.getReservedCasualLeaves();
                if (available < days)
                    throw new InsufficientLeaveBalanceException("Insufficient casual leave balance");
                lb.setReservedCasualLeaves(lb.getReservedCasualLeaves() + days);
            }
            case EARNED -> {
                BigDecimal available = lb.getEarnedLeaveBalance()
                        .subtract(BigDecimal.valueOf(lb.getReservedEarnedLeaves()));
                if (available.compareTo(BigDecimal.valueOf(days)) < 0)
                    throw new InsufficientLeaveBalanceException("Insufficient earned leave balance");
                lb.setReservedEarnedLeaves(lb.getReservedEarnedLeaves() + days);
            }
            case COMP_OFF -> {
                int available = lb.getCompoffBalance() - lb.getReservedComoffLeaves();
                if (available < days)
                    throw new InsufficientLeaveBalanceException("Insufficient comp-off leave balance");
                lb.setReservedComoffLeaves(lb.getReservedComoffLeaves() + days);
            }
            default -> { }
        }
    }

    @Override
    @Transactional
    public void releaseReservedLeaves(Long userId, int year, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;

        LeaveBalance lb = findByEmployeeAndYear(userId, year);

        switch (type) {
            case SICK   -> lb.setReservedSickLeaves(Math.max(0, lb.getReservedSickLeaves() - days));
            case CASUAL -> lb.setReservedCasualLeaves(Math.max(0, lb.getReservedCasualLeaves() - days));
            case EARNED -> lb.setReservedEarnedLeaves(Math.max(0, lb.getReservedEarnedLeaves() - days));
            case COMP_OFF -> lb.setReservedComoffLeaves(Math.max(0, lb.getReservedComoffLeaves() - days));
            default -> { }
        }
    }

    @Override
    @Transactional
    public void applyApproval(Long userId, int year, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;

        LeaveBalance lb = findByEmployeeAndYear(userId, year);

        switch (type) {
            case SICK -> {
                if (lb.getReservedSickLeaves() < days)
                    throw new InvalidLeaveStateException(
                            "Invalid leave state: reserved sick leaves less than requested days");
                lb.setReservedSickLeaves(lb.getReservedSickLeaves() - days);
                lb.setSickLeaveBalance(lb.getSickLeaveBalance() - days);
                lb.setSickLeavesConsumed(lb.getSickLeavesConsumed() + days);
            }
            case CASUAL -> {
                if (lb.getReservedCasualLeaves() < days)
                    throw new InvalidLeaveStateException(
                            "Invalid leave state: reserved casual leaves less than requested days");
                lb.setReservedCasualLeaves(lb.getReservedCasualLeaves() - days);
                lb.setCasualLeaveBalance(lb.getCasualLeaveBalance() - days);
                lb.setCasualLeavesConsumed(lb.getCasualLeavesConsumed() + days);
            }
            case EARNED -> {
                if (lb.getReservedEarnedLeaves() < days)
                    throw new InvalidLeaveStateException(
                            "Invalid leave state: reserved earned leaves less than requested days");
                lb.setReservedEarnedLeaves(lb.getReservedEarnedLeaves() - days);
                lb.setEarnedLeaveBalance(lb.getEarnedLeaveBalance().subtract(BigDecimal.valueOf(days)));
                lb.setEarnedLeavesConsumed(lb.getEarnedLeavesConsumed() + days);
            }
            case COMP_OFF -> {
                if (lb.getReservedComoffLeaves() < days)
                    throw new InvalidLeaveStateException(
                            "Invalid leave state: reserved comp-off leaves less than requested days");
                lb.setReservedComoffLeaves(lb.getReservedComoffLeaves() - days);
                lb.setCompoffBalance(lb.getCompoffBalance() - days);
                lb.setCompoffLeavesConsumed(lb.getCompoffLeavesConsumed() + days);
            }
            default -> { }
        }
    }

    @Override
    @Transactional
    public void applyCancellationApproval(Long userId, int year, LeaveType type, int days) {
        if (type == LeaveType.UNPAID) return;

        LeaveBalance lb = findByEmployeeAndYear(userId, year);

        switch (type) {
            case SICK -> {
                lb.setSickLeaveBalance(lb.getSickLeaveBalance() + days);
                lb.setSickLeavesConsumed(lb.getSickLeavesConsumed() - days);
            }
            case CASUAL -> {
                lb.setCasualLeaveBalance(lb.getCasualLeaveBalance() + days);
                lb.setCasualLeavesConsumed(lb.getCasualLeavesConsumed() - days);
            }
            case EARNED -> {
                lb.setEarnedLeaveBalance(lb.getEarnedLeaveBalance().add(BigDecimal.valueOf(days)));
                lb.setEarnedLeavesConsumed(lb.getEarnedLeavesConsumed() - days);
            }
            case COMP_OFF -> {
                lb.setCompoffBalance(lb.getCompoffBalance() + days);
                lb.setCompoffLeavesConsumed(lb.getCompoffLeavesConsumed() - days);
            }
            default -> { }
        }
    }

    @Override
    @Transactional
    public void depositCompOff(Long userId, int year, int earnedMinutes) {
        LeaveBalance lb = findByEmployeeAndYear(userId, year);
        int daysEarned = earnedMinutes / 480;

        lb.setCompoffBalance(lb.getCompoffBalance() + daysEarned);
        leaveBalanceRepository.save(lb);
    }

    @Override
    public LeaveBalanceResponse getMyLeaveBalance(Integer year) {
        Employee me = securityUtils.getLoggedInEmployee();
        int targetYear = (year != null) ? year : Year.now().getValue();

        LeaveBalance lb = findByEmployeeAndYear(me.getUserId(), targetYear);
        return leaveBalanceMapper.mapToResponse(lb);
    }

    @Override
    public LeaveBalanceResponse getEmployeeLeaveBalance(Long employeeId, Integer year) {
        int targetYear = (year != null) ? year : Year.now().getValue();

        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + employeeId));

        LeaveBalance lb = findByEmployeeAndYear(employeeId, targetYear);
        return leaveBalanceMapper.mapToResponse(lb);
    }

    @Override
    public List<LeaveBalanceResponse> getTeamLeaveBalances(Integer year) {
        Employee manager = securityUtils.getLoggedInEmployee();
        int targetYear = (year != null) ? year : Year.now().getValue();

        return leaveBalanceRepository
                .findByEmployeeManagerUserIdAndYear(manager.getUserId(), targetYear)
                .stream()
                .map(leaveBalanceMapper::mapToResponse)
                .toList();
    }

    private LeaveBalance findByEmployeeAndYear(Long userId, int year) {
        return leaveBalanceRepository.findByEmployeeUserIdAndYear(userId, year)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Leave balance not found for employee/year"));
    }
}