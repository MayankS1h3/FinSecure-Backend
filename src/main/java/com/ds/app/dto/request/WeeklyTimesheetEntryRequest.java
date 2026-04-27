package com.ds.app.dto.request;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyTimesheetEntryRequest {
	@NotNull
	@Min(0)
	@Max(5)
	Integer weekNumber;
	
	@Size(min = 1, message = "At least one entry is required")
	List<TimesheetEntryRequest> entries;
}
