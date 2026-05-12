package com.ds.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ds.app.dto.response.ProjectResponse;
import com.ds.app.entity.Project;

public interface IProjectRepository extends JpaRepository<Project, Long>{
	
	@Query("""
			select new ProjectResponse(
				p.projectId,
				p.projectName
			)
			from EmployeeProject ep
			where 
			ep.employee.userId = :employeeId
			and
			ep.project.status = ProjectStatus.ACTIVE
			""")
	List<ProjectResponse> findByStatusAndEmployeeId(@Param("employeeId") Long employeeId);
}
