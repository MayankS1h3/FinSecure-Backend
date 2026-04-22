package com.ds.app.service.impl;

import com.ds.app.dto.request.TimesheetEntryRequest;
import com.ds.app.dto.request.WeeklyTimesheetEntryRequest;
import com.ds.app.dto.response.TimesheetEntryResponse;
import com.ds.app.dto.response.WeeklyTimesheetEntryResponse;
import com.ds.app.entity.Employee;
import com.ds.app.entity.Timesheet;
import com.ds.app.entity.TimesheetEntry;
import com.ds.app.enums.TimesheetStatus;
import com.ds.app.exception.DailyHoursLimitExceededException;
import com.ds.app.exception.InvalidTimesheetStateException;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.TimesheetEntryMapper;
import com.ds.app.repository.ITimesheetEntryRepository;
import com.ds.app.repository.ITimesheetRepository;
import com.ds.app.service.ITimesheetEntryService;
import com.ds.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetEntryServiceImpl implements ITimesheetEntryService {

    private final ITimesheetEntryRepository entryRepository;
    private final ITimesheetRepository timesheetRepository;
    private final TimesheetEntryMapper timesheetEntryMapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public TimesheetEntryResponse addMyEntry(TimesheetEntryRequest request) {
        Employee me = securityUtils.getLoggedInEmployee();

        int month = request.getDate().getMonthValue();
        int year = request.getDate().getYear();

        log.info("Add timesheet entry requested. employeeId={}, date={}, month={}, year={}",
                me.getUserId(), request.getDate(), month, year);

        Timesheet timesheet = timesheetRepository
                .findByEmployeeUserIdAndMonthAndYear(me.getUserId(), month, year)
                .orElseGet(() -> {
                    log.debug("No timesheet found. Creating new DRAFT timesheet. employeeId={}, month={}, year={}",
                            me.getUserId(), month, year);
                    return timesheetRepository.save(
                            Timesheet.builder()
                                    .employee(me)
                                    .month(month)
                                    .year(year)
                                    .status(TimesheetStatus.DRAFT)
                                    .totalMonthlyMinutes(0)
                                    .build()
                    );
                });

        ensureEditableAndResetIfRejected(timesheet);

        TimesheetEntry entry = timesheetEntryMapper.mapToEntity(request, timesheet);
        TimesheetEntry saved = entryRepository.save(entry);

        recalculateTotalHours(timesheet);

        log.info("Timesheet entry added. employeeId={}, timesheetId={}, entryId={}",
                me.getUserId(), timesheet.getTimesheetId(), saved.getTimesheetEntryId());

        return timesheetEntryMapper.mapToResponse(saved);
    }
    
    @Override
	public WeeklyTimesheetEntryResponse addWeeklyEntry(WeeklyTimesheetEntryRequest request) {
		Employee loggeInEmployee = securityUtils.getLoggedInEmployee();
		
		int month = request.getEntries().get(0).getDate().getMonthValue();
		int year = request.getEntries().get(0).getDate().getYear();
		
		Timesheet timesheet = timesheetRepository
				.findByEmployeeUserIdAndMonthAndYear(loggeInEmployee.getUserId(), month, year)
				.orElseGet( () -> timesheetRepository.save(
						Timesheet.builder()
						.employee(loggeInEmployee)
						.month(month)
						.year(year)
						.status(TimesheetStatus.DRAFT)
						.totalMonthlyMinutes(0)
						.build()
						));
		
        ensureEditableAndResetIfRejected(timesheet);

        List<TimesheetEntry> entries = timesheetEntryMapper.mapToEntityList(request, timesheet);
        
        validateDailyHours(entries);
        
        List<TimesheetEntry> savedEntries = entryRepository.saveAll(entries);
	}

    @Override
    public List<TimesheetEntryResponse> getMyEntries(Integer month, Integer year) {
        Employee me = securityUtils.getLoggedInEmployee();
        YearMonth ym = YearMonth.of(year, month);

        log.info("Fetch my entries by month/year requested. employeeId={}, month={}, year={}",
                me.getUserId(), month, year);

        List<TimesheetEntryResponse> response = entryRepository
                .findByTimesheetEmployeeUserIdAndDateBetweenOrderByDateAsc(
                        me.getUserId(),
                        ym.atDay(1),
                        ym.atEndOfMonth()
                ).stream()
                .map(timesheetEntryMapper::mapToResponse)
                .toList();

        log.debug("Fetch my entries by month/year completed. employeeId={}, count={}",
                me.getUserId(), response.size());

        return response;
    }

    @Override
    public List<TimesheetEntryResponse> getMyEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        Employee me = securityUtils.getLoggedInEmployee();

        log.info("Fetch my entries by date range requested. employeeId={}, startDate={}, endDate={}",
                me.getUserId(), startDate, endDate);

        List<TimesheetEntryResponse> response = entryRepository
                .findByTimesheetEmployeeUserIdAndDateBetweenOrderByDateAsc(
                        me.getUserId(), startDate, endDate
                ).stream()
                .map(timesheetEntryMapper::mapToResponse)
                .toList();

        log.debug("Fetch my entries by date range completed. employeeId={}, count={}",
                me.getUserId(), response.size());

        return response;
    }

    @Override
    @Transactional
    public TimesheetEntryResponse updateMyEntry(Long entryId, TimesheetEntryRequest request) {
        Employee me = securityUtils.getLoggedInEmployee();

        log.info("Update timesheet entry requested. employeeId={}, entryId={}", me.getUserId(), entryId);

        TimesheetEntry existing = entryRepository.findByTimesheetEntryIdAndTimesheetEmployeeUserId(entryId, me.getUserId())
                .orElseThrow(() -> {
                    log.warn("Timesheet entry not found for update. employeeId={}, entryId={}",
                            me.getUserId(), entryId);
                    return new ResourceNotFoundException("Timesheet entry not found with id: " + entryId);
                });

        Timesheet timesheet = existing.getTimesheet();
        ensureEditableAndResetIfRejected(timesheet);

        if (request.getDate().getMonthValue() != timesheet.getMonth()
                || request.getDate().getYear() != timesheet.getYear()) {
            log.warn("Entry date moved outside timesheet month/year. employeeId={}, entryId={}, requestDate={}, timesheetMonth={}, timesheetYear={}",
                    me.getUserId(), entryId, request.getDate(), timesheet.getMonth(), timesheet.getYear());
            throw new IllegalArgumentException("Entry date must remain within same timesheet month/year.");
        }

        int newTotalMinutes = (request.getHours() * 60) + request.getMinutes();

        existing.setDate(request.getDate());
        existing.setTaskDescription(request.getTaskDescription());
        existing.setTotalMinutesWorked(newTotalMinutes);
        existing.setProjectId(request.getProjectId());
        existing.setProjectName(request.getProjectName());

        TimesheetEntry saved = entryRepository.save(existing);
        recalculateTotalHours(timesheet);

        log.info("Timesheet entry updated. employeeId={}, timesheetId={}, entryId={}, totalMinutes={}",
                me.getUserId(), timesheet.getTimesheetId(), entryId, newTotalMinutes);

        return timesheetEntryMapper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMyEntry(Long entryId) {
        Employee me = securityUtils.getLoggedInEmployee();

        log.info("Delete timesheet entry requested. employeeId={}, entryId={}", me.getUserId(), entryId);

        TimesheetEntry existing = entryRepository.findByTimesheetEntryIdAndTimesheetEmployeeUserId(entryId, me.getUserId())
                .orElseThrow(() -> {
                    log.warn("Timesheet entry not found for delete. employeeId={}, entryId={}",
                            me.getUserId(), entryId);
                    return new ResourceNotFoundException("Timesheet entry not found with id: " + entryId);
                });

        Timesheet timesheet = existing.getTimesheet();
        ensureEditableAndResetIfRejected(timesheet);

        entryRepository.delete(existing);
        recalculateTotalHours(timesheet);

        log.info("Timesheet entry deleted. employeeId={}, timesheetId={}, entryId={}",
                me.getUserId(), timesheet.getTimesheetId(), entryId);
    }
    
    private void validateDailyHours(List<TimesheetEntry> entries) {
    	entries.forEach(entry -> {
    		if(entry.getTotalMinutesWorked() > 540) {
    			throw new DailyHoursLimitExceededException("Daily hours can not be more than 9");
    		}
    	});
    }
    
    private void validateDateRange(WeeklyTimesheetEntryRequest request) {
    	int month = request.getEntries().get(0).getDate().getMonthValue();
    	int year = request.getEntries().get(0).getDate().getYear();
    	
    	YearMonth ym = YearMonth.of(year, month);
    	int totalDaysInMonth = ym.lengthOfMonth();
    	
    	int weekNumber = request.getWeekNumber();
    	
    	LocalDate firstSunday = LocalDate.of(year, month, 1);
    	
    	while(firstSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
    		firstSunday.plusDays(1);
    	}
    	
    	LocalDate weekStartDate;
    	LocalDate weekEndDate;
    	
    	if(request.getWeekNumber() == 0) {
    		weekStartDate = LocalDate.of(year, month, 1);
    		weekEndDate = firstSunday;
    	}else {
    		int datesToAddInStartDate = firstSunday.getDayOfMonth() + 7 * (weekNumber - 1);
    		weekStartDate = LocalDate.of(year, month, 1 + datesToAddInStartDate);
    		
    		int datesToAddInEndDate = ( firstSunday.getDayOfMonth() + 7 * weekNumber ) > totalDaysInMonth : ;
    		
    		weekEndDate = Local
    	}
    }

    private void ensureEditableAndResetIfRejected(Timesheet timesheet) {
        if (timesheet.getStatus() == TimesheetStatus.SUBMITTED || timesheet.getStatus() == TimesheetStatus.APPROVED) {
            log.warn("Timesheet not editable. timesheetId={}, status={}", timesheet.getTimesheetId(), timesheet.getStatus());
            throw new InvalidTimesheetStateException("Cannot modify entries. Timesheet is already SUBMITTED/APPROVED");
        }

        if (timesheet.getStatus() == TimesheetStatus.REJECTED) {
            log.info("Rejected timesheet reset to DRAFT before edit. timesheetId={}", timesheet.getTimesheetId());
            timesheet.setStatus(TimesheetStatus.DRAFT);
            timesheet.setSubmittedAt(null);
            timesheet.setApprovedBy(null);
            timesheet.setApprovalDate(null);
            timesheet.setRejectionReason(null);
            timesheetRepository.save(timesheet);
        }
    }

    private void recalculateTotalHours(Timesheet timesheet) {
        int total = entryRepository.findByTimesheetTimesheetIdOrderByDateAsc(timesheet.getTimesheetId())
                .stream()
                .map(TimesheetEntry::getTotalMinutesWorked)
                .filter(m -> m != null)
                .reduce(0, Integer::sum);

        timesheet.setTotalMonthlyMinutes(total);
        timesheetRepository.save(timesheet);

        log.debug("Timesheet total recalculated. timesheetId={}, totalMonthlyMinutes={}",
                timesheet.getTimesheetId(), total);
    }
}