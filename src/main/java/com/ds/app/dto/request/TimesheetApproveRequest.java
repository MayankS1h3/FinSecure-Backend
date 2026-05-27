package com.ds.app.dto.request;

import com.ds.app.enums.CompensationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TimesheetApproveRequest {

    // 1. Used when the manager clicks "Reject" for the entire timesheet
    private String rejectionReason;

    // 2. Used when the manager clicks "Approve" but wants to change specific days
    @Valid
    private List<ManagerOverrideDTO> managerOverrides;

    @Data
    public static class ManagerOverrideDTO {
        @NotNull(message = "Date cannot be null")
        private LocalDate date;

        @NotNull(message = "Final decision must be provided")
        private CompensationMode finalDecision;

        // Used to justify why the manager changed this specific day's compensation
        private String overrideReason;
    }
}