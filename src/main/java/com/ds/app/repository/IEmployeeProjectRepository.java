package com.ds.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.app.entity.Project;

public interface IEmployeeProjectRepository extends JpaRepository<Project, Long>{

}
