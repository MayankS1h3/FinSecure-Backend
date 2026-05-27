package com.ds.app.dto.request;

import com.ds.app.enums.CompensationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TimesheetSubmitRequest {

    @Valid
    private List<EmployeeChoiceDTO> employeeChoices;

    @Data
    public static class EmployeeChoiceDTO {
        @NotNull(message = "Date cannot be null")
        private LocalDate date;

        @NotNull(message = "Preference must be selected")
        private CompensationMode preference;
    }
}