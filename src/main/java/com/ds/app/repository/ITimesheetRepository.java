package com.ds.app.repository;

import com.ds.app.entity.Timesheet;
import com.ds.app.enums.TimesheetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ITimesheetRepository extends JpaRepository<Timesheet, Long> {

    Optional<Timesheet> findByEmployee_UserIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    Optional<Timesheet> findByTimesheetIdAndEmployeeUserId(Long timesheetId, Long employeeId);

    Page<Timesheet> findByEmployee_Manager_UserIdAndStatus(Long managerId, TimesheetStatus status, Pageable pageable);

    Page<Timesheet> findByEmployee_Manager_UserIdAndMonthAndYear(Long managerId, Integer month, Integer year, Pageable pageable);
    
    Boolean existsByEmployee_UserIdAndMonthAndYear(Long employeeId, Integer month, Integer year);
}