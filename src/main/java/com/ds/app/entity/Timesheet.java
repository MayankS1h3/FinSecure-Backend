package com.ds.app.entity;

import com.ds.app.enums.TimesheetStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
		uniqueConstraints = @UniqueConstraint(
				columnNames = {"employee_id", "month", "year"}
				)
		)
public class Timesheet {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long timesheetId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

    @Column(nullable = false)
	private Integer month;

    @Column(nullable = false)
	private Integer year;
	
	@Enumerated(EnumType.STRING)
	@Builder.Default
    @Column(nullable = false)
	private TimesheetStatus status = TimesheetStatus.DRAFT;
	
	private LocalDateTime submittedAt;
	
	@Builder.Default
    @Column(nullable = false)
	private Integer totalMonthlyMinutes = 0;
	
//	@OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
//	private List<TimesheetEntry> timesheetEntries;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approved_by")
	private Employee approvedBy;
	
	private LocalDate approvalDate;
	private String rejectionReason;
	
}
