package com.ds.app.mapper;

import com.ds.app.dto.request.TimesheetEntryRequest;
import com.ds.app.dto.request.WeeklyTimesheetEntryRequest;
import com.ds.app.dto.response.TimesheetEntryResponse;
import com.ds.app.dto.response.WeeklyTimesheetEntryResponse;
import com.ds.app.entity.Employee;
import com.ds.app.entity.Timesheet;
import com.ds.app.entity.TimesheetEntry;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class TimesheetEntryMapper {

    public TimesheetEntry mapToEntity(TimesheetEntryRequest request, Employee employee) {

        int totalMinutes = (request.getHours() * 60) + request.getMinutes();

        return TimesheetEntry.builder()
//                .timesheet(timesheet)
        		.employee(employee)
                .date(request.getDate())
                .taskDescription(request.getTaskDescription())
                .totalMinutesWorked(totalMinutes)
                .projectId(request.getProjectId())
                .projectName(request.getProjectName())
                .build();
    }

    public TimesheetEntryResponse mapToResponse(TimesheetEntry entry) {

        int hours = entry.getTotalMinutesWorked() / 60;
        int minutes = entry.getTotalMinutesWorked() % 60;

        return TimesheetEntryResponse.builder()
                .timesheetEntryId(entry.getTimesheetEntryId())
                .employeeId(entry.getEmployee().getUserId())
//                .timesheetId(entry.getTimesheet().getTimesheetId())
                .date(entry.getDate())
                .taskDescription(entry.getTaskDescription())
                .totalMinutesWorked(entry.getTotalMinutesWorked()) 
                .formattedTime(String.format("%02d:%02d", hours, minutes)) 
                .projectId(entry.getProjectId())
                .projectName(entry.getProjectName())
                .build();
    }
    
    public List<TimesheetEntry> mapToEntityList(WeeklyTimesheetEntryRequest request, Employee employee){
    	return request.getEntries()
    			.stream()
    			.map(entry -> mapToEntity(entry, employee))
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