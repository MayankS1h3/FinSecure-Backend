package com.ds.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
		name = "leave_balance",
		uniqueConstraints = @UniqueConstraint(
				columnNames = {"employee_id", "year"})
		
		)
public class LeaveBalance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long balanceId;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

    @Column(nullable = false)
	private Integer year;
	
	@Builder.Default
	private Integer sickLeaveBalance = 10;
	
	@Builder.Default
	private Integer casualLeaveBalance = 8;
	
	@Builder.Default
	private BigDecimal earnedLeaveBalance = BigDecimal.valueOf(0);

    @Builder.Default
    private Integer compoffBalance = 0;

    @Builder.Default
    private Integer reservedSickLeaves = 0;

    @Builder.Default
    private Integer reservedCasualLeaves = 0;

    @Builder.Default
    private Integer reservedEarnedLeaves = 0;

    @Builder.Default
    private Integer reservedComoffLeaves = 0;

    @Builder.Default
    private Integer sickLeavesConsumed = 0;

    @Builder.Default
    private Integer casualLeavesConsumed = 0;

    @Builder.Default
    private Integer earnedLeavesConsumed = 0;

    @Builder.Default
    private Integer compoffLeavesConsumed = 0;
	
	@Builder.Default
	private BigDecimal carriedForwardEarnedDays = BigDecimal.valueOf(0);
}
