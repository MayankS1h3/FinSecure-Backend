package com.ds.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntryRequest {
	
	@NotNull(message = "Date is required")
	@PastOrPresent(message = "Cannot make entry for future dates")
	private LocalDate date;
	
	@NotBlank(message = "Task description is required")
	private String taskDescription;

    @NotNull(message = "Hours are required")
    @Min(value = 0, message = "Hours cannot be negative")
    @Max(value = 9, message = "Hours cannot exceed 9")
    private Integer hours;

    @NotNull(message = "Minutes are required")
    @Min(value = 0, message = "Minutes cannot be negative")
    @Max(value = 59, message = "Minutes must be between 0 and 59")
    private Integer minutes;
	
	private Long projectId;
	
	@NotBlank(message = "Project name is required")
	private String projectName;
}
