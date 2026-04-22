package com.ds.app.dto.request;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
	Integer weekNumber;
	
	@Min(1)
	List<TimesheetEntryRequest> entries;
}
