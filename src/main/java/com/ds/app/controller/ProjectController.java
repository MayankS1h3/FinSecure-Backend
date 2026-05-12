package com.ds.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.app.dto.response.ProjectResponse;
import com.ds.app.service.IProjectService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {
	
	private final IProjectService projectService;
	
	@GetMapping()
	public ResponseEntity<List<ProjectResponse>> getProjectListForTimesheetEntryDropdown() {
		List<ProjectResponse> responseList = projectService.findProjectForTimesheetEntryDropdown();
		return ResponseEntity.ok(responseList);
	}
}
