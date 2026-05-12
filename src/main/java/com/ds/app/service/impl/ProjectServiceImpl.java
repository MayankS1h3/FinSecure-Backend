package com.ds.app.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ds.app.dto.response.ProjectResponse;
import com.ds.app.entity.Employee;
import com.ds.app.repository.IProjectRepository;
import com.ds.app.service.IProjectService;
import com.ds.app.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements IProjectService{
	
	private final SecurityUtils securityUtil;
	private final IProjectRepository projectRepo;

	@Override
	public List<ProjectResponse> findProjectForTimesheetEntryDropdown() {
		Employee loggedInEmployee = securityUtil.getLoggedInEmployee();
		
		return projectRepo.findByStatusAndEmployeeId(loggedInEmployee.getUserId());
	}

}
