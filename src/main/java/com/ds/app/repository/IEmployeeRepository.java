package com.ds.app.repository;

import com.ds.app.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IEmployeeRepository extends JpaRepository<Employee, Long>{
	Optional<Employee> findByUserId(Long userId);
	Optional<Employee> findByUsername(String username);

    @Query("""
        SELECT e FROM Employee e 
        WHERE e.isActive = true 
        AND NOT EXISTS (
            SELECT a FROM Attendance a 
            WHERE a.employee = e 
            AND a.date = :date
        ) 
        AND NOT EXISTS (
            SELECT l FROM Leave l 
            WHERE l.employee = e 
            AND l.status = com.ds.app.enums.LeaveStatus.APPROVED 
            AND :date BETWEEN l.startDate AND l.endDate
        )
        """)
    List<Employee> findAbsentEmployeesByDate(@Param("date") LocalDate date);
}
