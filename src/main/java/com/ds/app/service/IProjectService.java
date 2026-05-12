package com.ds.app.service;

import java.util.List;

import com.ds.app.dto.response.ProjectResponse;

public interface IProjectService {
	List<ProjectResponse> findProjectForTimesheetEntryDropdown();
}
