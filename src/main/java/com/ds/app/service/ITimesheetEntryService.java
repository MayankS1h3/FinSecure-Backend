package com.ds.app.service;

import com.ds.app.dto.request.TimesheetEntryRequest;
import com.ds.app.dto.request.WeeklyTimesheetEntryRequest;
import com.ds.app.dto.response.TimesheetEntryResponse;
import com.ds.app.dto.response.WeeklyTimesheetEntryResponse;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ITimesheetEntryService {
    TimesheetEntryResponse addMyEntry(TimesheetEntryRequest request);
    
    Page<TimesheetEntryResponse> getMyEntries(Integer month, Integer year, Pageable pageable);
    
    List<TimesheetEntryResponse> getMyEntriesByDateRange(LocalDate startDate, LocalDate endDate);
    
    TimesheetEntryResponse updateMyEntry(Long entryId, TimesheetEntryRequest request);
    
    void deleteMyEntry(Long entryId);
    
    WeeklyTimesheetEntryResponse addWeeklyEntry(WeeklyTimesheetEntryRequest request);
}