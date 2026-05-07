package com.ds.app.repository;

import com.ds.app.dto.response.EmployeeProjectHoursRow;
import com.ds.app.entity.TimesheetEntry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Repository
public interface ITimesheetEntryRepository extends JpaRepository<TimesheetEntry, Long> {

	List<TimesheetEntry> findByEmployee_UserIdAndDateBetweenOrderByDateAsc(Long employeeId, LocalDate startDate,
			LocalDate endDate);

//	Optional<TimesheetEntry> findByTimesheetEntryIdAndTimesheetEmployeeUserId(Long entryId, Long employeeId);

//	List<TimesheetEntry> findByTimesheetTimesheetIdOrderByDateAsc(Long timesheetId);
	
	@Query("""
			select te
			from TimesheetEntry te
			join te.employee e
			where e.userId = :employeeId
			and Month(te.date) = :month
			and Year(te.date) = :year
			order by te.date asc
			""")
	Page<TimesheetEntry> findByEmployee_UserIdAndMonthAndYear(
			@Param("employeeId") Long employeeId,
			@Param("month") Integer month,
			@Param("year") Integer year,
			Pageable pageable);

	@Query("""
			    select new com.ds.app.dto.response.EmployeeProjectHoursRow(
			        e.userId,
			        concat(e.firstName, ' ', e.lastName),
			        coalesce(sum(te.totalMinutesWorked), 0) / 60.0
			    )
			    from TimesheetEntry te
			    join te.employee e
			    where e.manager.userId = :managerId
			      and te.projectId = :projectId
			      and month(te.date) = :month
			      and year(te.date) = :year
			    group by e.userId, e.firstName, e.lastName
			""")
	List<EmployeeProjectHoursRow> findEmployeeWiseProjectHoursForManager(@Param("month") Integer month,
			@Param("year") Integer year, @Param("projectId") Long projectId, @Param("managerId") Long managerId);
	
//	List<TimesheetEntry> findByTimesheet_TimesheetIdAndDateIn(Long timesheetId, Set<LocalDate> dates);
	List<TimesheetEntry> findByEmployee_UserIdAndDateIn(Long employeeId, Set<LocalDate> dates);

}