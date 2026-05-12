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
import com.ds.app.exception.InvalidDateForTheWeek;
import com.ds.app.exception.InvalidTimesheetStateException;
import com.ds.app.exception.ResourceNotFoundException;
import com.ds.app.mapper.TimesheetEntryMapper;
import com.ds.app.repository.ITimesheetEntryRepository;
import com.ds.app.repository.ITimesheetRepository;
import com.ds.app.service.ISystemConfigurationService;
import com.ds.app.service.ITimesheetEntryService;
import com.ds.app.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimesheetEntryServiceImpl implements ITimesheetEntryService {

    private final ITimesheetEntryRepository entryRepository;
    private final ITimesheetRepository timesheetRepository;
    private final TimesheetEntryMapper timesheetEntryMapper;
    private final SecurityUtils securityUtils;
    private final ISystemConfigurationService configService;
    
//    @Value("${app.MAX_HOURS_PER_DAY}")
//    private int MAX_HOURS_PER_DAY;

    @Override
    @Transactional
    public TimesheetEntryResponse addMyEntry(TimesheetEntryRequest request) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        int month = request.getDate().getMonthValue();
        int year = request.getDate().getYear();

        log.info("Add timesheet entry requested. employeeId={}, date={}, month={}, year={}",
                loggedInEmployee.getUserId(), request.getDate(), month, year);

        Timesheet timesheet = timesheetRepository
                .findByEmployee_UserIdAndMonthAndYear(loggedInEmployee.getUserId(), month, year)
                .orElseGet(() -> {
                    log.debug("No timesheet found. Creating new DRAFT timesheet. employeeId={}, month={}, year={}",
                    		loggedInEmployee.getUserId(), month, year);
                    return timesheetRepository.save(
                            Timesheet.builder()
                                    .employee(loggedInEmployee)
                                    .month(month)
                                    .year(year)
                                    .status(TimesheetStatus.DRAFT)
                                    .totalMonthlyMinutes(0)
                                    .build()
                    );
                });

        ensureEditableAndResetIfRejected(timesheet);

        TimesheetEntry entry = timesheetEntryMapper.mapToEntity(request, loggedInEmployee);
//        validateDailyHours(List.of(entry),loggedInEmployee);
        TimesheetEntry saved = entryRepository.save(entry);

        recalculateTotalHours(timesheet, loggedInEmployee);

        log.info("Timesheet entry added. employeeId={}, timesheetId={}, entryId={}",
                loggedInEmployee.getUserId(), timesheet.getTimesheetId(), saved.getTimesheetEntryId());

        return timesheetEntryMapper.mapToResponse(saved);
    }
    
    @Override
    @Transactional
	public WeeklyTimesheetEntryResponse addWeeklyEntry(WeeklyTimesheetEntryRequest weekyRequest) {
		Employee loggeInEmployee = securityUtils.getLoggedInEmployee();

        validateWeeklyDateRange(weekyRequest);
		
		int month = weekyRequest.getEntries().get(0).getDate().getMonthValue();
		int year = weekyRequest.getEntries().get(0).getDate().getYear();
		
		Timesheet timesheet = timesheetRepository
				.findByEmployee_UserIdAndMonthAndYear(loggeInEmployee.getUserId(), month, year)
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

        List<TimesheetEntry> entries = timesheetEntryMapper.mapToEntityList(weekyRequest, loggeInEmployee);
        
//        validateDailyHours(entries, loggeInEmployee);
        
        List<TimesheetEntry> savedEntries = entryRepository.saveAll(entries);

        recalculateTotalHours(timesheet, loggeInEmployee);

        return timesheetEntryMapper
                .mapToWeeklyResponse(savedEntries);
	}

    @Override
    public Page<TimesheetEntryResponse> getMyEntries(Integer month, Integer year, Pageable pageable) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();
//        YearMonth ym = YearMonth.of(year, month);

        log.info("Fetch my entries by month/year requested. employeeId={}, month={}, year={}",
                loggedInEmployee.getUserId(), month, year);

        Page<TimesheetEntryResponse> response = entryRepository
                .findByEmployee_UserIdAndMonthAndYear(
                        loggedInEmployee.getUserId(),
                        month,
                        year,
                        pageable
                ).map(timesheetEntryMapper::mapToResponse);
                

        log.debug("Fetch my entries by month/year completed. employeeId={}, count={}",
                loggedInEmployee.getUserId(), response.getSize());

        return response;
    }

    @Override
    public List<TimesheetEntryResponse> getMyEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        log.info("Fetch my entries by date range requested. employeeId={}, startDate={}, endDate={}",
                loggedInEmployee.getUserId(), startDate, endDate);

        List<TimesheetEntryResponse> response = entryRepository
                .findByEmployee_UserIdAndDateBetweenOrderByDateAsc(
                        loggedInEmployee.getUserId(), startDate, endDate
                ).stream()
                .map(timesheetEntryMapper::mapToResponse)
                .toList();

        log.debug("Fetch my entries by date range completed. employeeId={}, count={}",
                loggedInEmployee.getUserId(), response.size());

        return response;
    }

    @Override
    @Transactional
    public TimesheetEntryResponse updateMyEntry(Long entryId, TimesheetEntryRequest request) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        log.info("Update timesheet entry requested. employeeId={}, entryId={}", loggedInEmployee.getUserId(), entryId);

        TimesheetEntry existingEntry = entryRepository.findById(entryId)
                .orElseThrow(() -> {
                    log.warn("Timesheet entry not found for update. employeeId={}, entryId={}",
                            loggedInEmployee.getUserId(), entryId);
                    return new ResourceNotFoundException("Timesheet entry not found with id: " + entryId);
                });

        Timesheet timesheet = timesheetRepository.findByEmployee_UserIdAndMonthAndYear(loggedInEmployee.getUserId(),
        		existingEntry.getDate().getMonthValue(),
        		existingEntry.getDate().getYear())
        		.orElseThrow( () -> new ResourceNotFoundException("Timesheet Not Found!"));
        
        ensureEditableAndResetIfRejected(timesheet);

        if (request.getDate().getMonthValue() != timesheet.getMonth()
                || request.getDate().getYear() != timesheet.getYear()) {
            log.warn("Entry date moved outside timesheet month/year. employeeId={}, entryId={}, requestDate={}, timesheetMonth={}, timesheetYear={}",
                    loggedInEmployee.getUserId(), entryId, request.getDate(), timesheet.getMonth(), timesheet.getYear());
            throw new IllegalArgumentException("Entry date must remain within same timesheet month/year.");
        }

        int newTotalMinutes = (request.getHours() * 60) + request.getMinutes();

        existingEntry.setDate(request.getDate());
        existingEntry.setTaskDescription(request.getTaskDescription());
        existingEntry.setTotalMinutesWorked(newTotalMinutes);
        existingEntry.setProjectId(request.getProjectId());
        existingEntry.setProjectName(request.getProjectName());

        validateDailyHours(List.of(existingEntry), loggedInEmployee);

        TimesheetEntry saved = entryRepository.save(existingEntry);
        recalculateTotalHours(timesheet, loggedInEmployee);

        log.info("Timesheet entry updated. employeeId={}, timesheetId={}, entryId={}, totalMinutes={}",
                loggedInEmployee.getUserId(), timesheet.getTimesheetId(), entryId, newTotalMinutes);

        return timesheetEntryMapper.mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMyEntry(Long entryId) {
        Employee loggedInEmployee = securityUtils.getLoggedInEmployee();

        log.info("Delete timesheet entry requested. employeeId={}, entryId={}", loggedInEmployee.getUserId(), entryId);

        TimesheetEntry existingEntry = entryRepository.findById(entryId)
                .orElseThrow(() -> {
                    log.warn("Timesheet entry not found for delete. employeeId={}, entryId={}",
                    		loggedInEmployee.getUserId(), entryId);
                    return new ResourceNotFoundException("Timesheet entry not found with id: " + entryId);
                });

        Timesheet timesheet = timesheetRepository.findByEmployee_UserIdAndMonthAndYear(loggedInEmployee.getUserId(),
        		existingEntry.getDate().getMonthValue(),
        		existingEntry.getDate().getYear())
        		.orElseThrow( () -> new ResourceNotFoundException("Timesheet Not Found!"));
        
        ensureEditableAndResetIfRejected(timesheet);

        entryRepository.delete(existingEntry);
        recalculateTotalHours(timesheet, loggedInEmployee);

        log.info("Timesheet entry deleted. employeeId={}, timesheetId={}, entryId={}",
                loggedInEmployee.getUserId(), timesheet.getTimesheetId(), entryId);
    }
    
    private void validateDailyHours(List<TimesheetEntry> entries, Employee loggedInEmployee) {
    	Map<LocalDate, Integer> newEntriesMinutesByDate = entries.stream()
    														.collect(Collectors.groupingBy(entry -> entry.getDate(),
    														Collectors.summingInt(entry -> entry.getTotalMinutesWorked())));

        List<Long> entryIdsBeingProcessed = entries.stream()
                .map(TimesheetEntry::getTimesheetEntryId)
                .filter(id -> id != null)
                .toList();

        List<TimesheetEntry> existingEntries = entryRepository.findByEmployee_UserIdAndDateIn(loggedInEmployee.getUserId(), newEntriesMinutesByDate.keySet());

        Map<LocalDate, Integer> existingMinutesByDate = existingEntries.stream()
                .filter(entry -> !entryIdsBeingProcessed.contains(entry.getTimesheetEntryId()))
                .collect(Collectors.groupingBy(entry -> entry.getDate(),
                        Collectors.summingInt(entry -> entry.getTotalMinutesWorked())));

        for (Map.Entry<LocalDate, Integer> mapEntry : newEntriesMinutesByDate.entrySet()) {
            LocalDate date = mapEntry.getKey();
            int newMinutes = mapEntry.getValue();

            int existingMinutes = existingMinutesByDate.getOrDefault(date,0);

            if(newMinutes + existingMinutes > configService.getMaxHoursPerDay()) {
                throw new DailyHoursLimitExceededException("Daily hours can not be more than 9");
            }
        }
//    	for (Map.Entry<LocalDate, Integer> mapEntry : totalMinutesWorkedByDate.entrySet()) {
//			int existingMinutes = entryRepository.findByTimesheet_TimesheetIdAndDate(timesheet.getTimesheetId(), mapEntry.getKey())
//					.stream()
//					.map(entry -> entry.getTotalMinutesWorked())
//					.reduce(0, (a,b) -> a+b);
//
//			if(existingMinutes + mapEntry.getValue() > 540) {
//				throw new DailyHoursLimitExceededException("Daily hours can not be more than 9");
//			}
//		}

    }
    
    private void validateWeeklyDateRange(WeeklyTimesheetEntryRequest request) {
      
        LocalDate firstEntryDate = request.getEntries().get(0).getDate();
        int month = firstEntryDate.getMonthValue();
        int year = firstEntryDate.getYear();
        int weekNumber = request.getWeekNumber();

        LocalDate startOfMonth = LocalDate.of(year, month, 1);
        LocalDate endOfMonth = YearMonth.of(year, month).atEndOfMonth();

        int firstSundayDiffFromStart = (7 - startOfMonth.getDayOfWeek().getValue()) % 7;
        LocalDate firstSunday = startOfMonth.plusDays(firstSundayDiffFromStart);

        LocalDate startOfWeek;
        LocalDate endOfWeek;

        if (weekNumber == 0) {
            startOfWeek = startOfMonth;
            endOfWeek = firstSunday;
        } else {
            startOfWeek = firstSunday.plusDays(1 + 7 * (weekNumber - 1));
            endOfWeek = startOfWeek.plusDays(6);

            if (endOfWeek.isAfter(endOfMonth)) {
                endOfWeek = endOfMonth;
            }
        }

        for (TimesheetEntryRequest entry : request.getEntries()) {
            LocalDate date = entry.getDate();

            if (date.isBefore(startOfWeek) || date.isAfter(endOfWeek)) {
                throw new InvalidDateForTheWeek(
                    "Invalid date for this week number. Allowed range: " + startOfWeek + " to " + endOfWeek
                );
            }
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

    private void recalculateTotalHours(Timesheet timesheet, Employee loggedInEmployee) {
    	
        int total = entryRepository.findByEmployee_UserIdAndMonthAndYear(
        		loggedInEmployee.getUserId(),
        		timesheet.getMonth(),
        		timesheet.getYear(),
        		Pageable.unpaged())
        		.getContent()
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