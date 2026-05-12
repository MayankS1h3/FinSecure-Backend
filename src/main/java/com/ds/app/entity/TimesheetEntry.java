package com.ds.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimesheetEntry {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long timesheetEntryId;
	
//	@ManyToOne(fetch = FetchType.LAZY)
//	@JoinColumn(name = "timesheet_id", nullable = false)
//	private Timesheet timesheet;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

    @Column(nullable = false)
	private LocalDate date;

    @Column(nullable = false)
	private String taskDescription;

    @Column(nullable = false)
	private Integer totalMinutesWorked;

    @Column(nullable = false)
	private Long projectId;

    @Column(nullable = false)
	private String projectName;
  
}
