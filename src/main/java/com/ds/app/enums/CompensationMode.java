package com.ds.app.enums;

public enum CompensationMode {
    NOT_ELIGIBLE,    // No overtime allowed for this day type
    CASH_ONLY,       // All extra hours routed to Payroll
    COMP_OFF_ONLY,   // All extra hours banked as Leave
    EMPLOYEE_CHOICE, // Employee selects preference in the UI slider
    POLICY_SPLIT     // Automated overflow (e.g., first 8 hours to Comp-off, rest to Cash)
}