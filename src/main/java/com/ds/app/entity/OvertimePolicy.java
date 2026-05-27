package com.ds.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"country", "state", "name"})
)
public class OvertimePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long policyId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private Integer standardDailyMinutes;   // e.g., 480 (8 hours)

    @Column(nullable = false)
    private Integer standardWeeklyMinutes;  // e.g., 2400 (40 hours)

    // --- NESTED DAY RULES ---

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="mode", column=@Column(name="weekday_mode")),
            @AttributeOverride(name="minMinutesToQualify", column=@Column(name="weekday_min_mins")),
            @AttributeOverride(name="cashMultiplier", column=@Column(name="weekday_cash_mult")),
            @AttributeOverride(name="compOffMultiplier", column=@Column(name="weekday_comp_mult")),
            @AttributeOverride(name="maxCompOffMinutesBeforeSplit", column=@Column(name="weekday_split_mins"))
    })
    private DayTypeRule weekdayRule;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="mode", column=@Column(name="weekend_mode")),
            @AttributeOverride(name="minMinutesToQualify", column=@Column(name="weekend_min_mins")),
            @AttributeOverride(name="cashMultiplier", column=@Column(name="weekend_cash_mult")),
            @AttributeOverride(name="compOffMultiplier", column=@Column(name="weekend_comp_mult")),
            @AttributeOverride(name="maxCompOffMinutesBeforeSplit", column=@Column(name="weekend_split_mins"))
    })
    private DayTypeRule weekendRule;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name="mode", column=@Column(name="holiday_mode")),
            @AttributeOverride(name="minMinutesToQualify", column=@Column(name="holiday_min_mins")),
            @AttributeOverride(name="cashMultiplier", column=@Column(name="holiday_cash_mult")),
            @AttributeOverride(name="compOffMultiplier", column=@Column(name="holiday_comp_mult")),
            @AttributeOverride(name="maxCompOffMinutesBeforeSplit", column=@Column(name="holiday_split_mins"))
    })
    private DayTypeRule holidayRule;
}