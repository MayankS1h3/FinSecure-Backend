package com.ds.app.dto.response;

import com.ds.app.enums.CompensationMode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OvertimeResponse {
    private int extraMinutes;

    // The final calculated values to be deposited/paid
    private int cashMinutes;
    private int compOffMinutes;

    // Metadata for the UI and Audit Trail
    private CompensationMode appliedMode;
    private boolean requiresEmployeeChoice;
    private String message;
}