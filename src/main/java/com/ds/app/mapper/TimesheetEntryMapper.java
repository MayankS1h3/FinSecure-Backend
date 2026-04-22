package com.ds.app.mapper;

import com.ds.app.dto.request.TimesheetEntryRequest;
import com.ds.app.dto.request.WeeklyTimesheetEntryRequest;
import com.ds.app.dto.response.TimesheetEntryResponse;
import com.ds.app.dto.response.WeeklyTimesheetEntryResponse;
import com.ds.app.entity.Timesheet;
import com.ds.app.entity.TimesheetEntry;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TimesheetEntryMapper {

    public TimesheetEntry mapToEntity(TimesheetEntryRequest request, Timesheet timesheet) {

        int totalMinutes = (request.getHours() * 60) + request.getMinutes();

        return TimesheetEntry.builder()
                .timesheet(timesheet)
                .date(request.getDate())
                .taskDescription(request.getTaskDescription())
                .totalMinutesWorked(totalMinutes)
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .build();
    }

    public TimesheetEntryResponse mapToResponse(TimesheetEntry e) {

        int hours = e.getTotalMinutesWorked() / 60;
        int minutes = e.getTotalMinutesWorked() % 60;

        return TimesheetEntryResponse.builder()
                .timesheetEntryId(e.getTimesheetEntryId())
                .timesheetId(e.getTimesheet().getTimesheetId())
                .date(e.getDate())
                .taskDescription(e.getTaskDescription())
                .totalMinutesWorked(e.getTotalMinutesWorked()) 
                .formattedTime(String.format("%02d:%02d", hours, minutes)) 
                .projectId(e.getProjectId())
                .projectName(e.getProjectName())
                .build();
    }
    
    public List<TimesheetEntry> mapToEntityList(WeeklyTimesheetEntryRequest request, Timesheet timesheet){
    	return request.getEntries()
    			.stream()
    			.map(entry -> mapToEntity(entry, timesheet))
    			.toList();
    }
    
    public WeeklyTimesheetEntryResponse mapToWeeklyResponse(List<TimesheetEntry> entries) {
    	List<TimesheetEntryResponse> returnList =  entries.stream()
    			.map(entry -> mapToResponse(entry))
    			.toList();
    	
    	return WeeklyTimesheetEntryResponse.builder()
    			.responseList(returnList)
    			.build();
    }
}