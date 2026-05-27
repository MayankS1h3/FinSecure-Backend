package com.ds.app.service;

import com.ds.app.dto.response.LeaveBalanceResponse;
import com.ds.app.enums.LeaveType;

import java.util.List;

public interface ILeaveBalanceService {

    // ── internal methods called by LeaveService (existing) ──
    void reserveLeaves(Long userId, int year, LeaveType type, int days);
    void releaseReservedLeaves(Long userId, int year, LeaveType type, int days);
    void applyApproval(Long userId, int year, LeaveType type, int days);
    void applyCancellationApproval(Long userId, int year, LeaveType type, int days);
    void depositCompOff(Long userId, int year, int earnedMinutes);
    // ── read methods exposed via controller (new) ──

    // employee views own balance
    LeaveBalanceResponse getMyLeaveBalance(Integer year);

    // manager views a specific employee's balance
    LeaveBalanceResponse getEmployeeLeaveBalance(Long employeeId, Integer year);

    // manager views all their team's balance for a year
    List<LeaveBalanceResponse> getTeamLeaveBalances(Integer year);
}