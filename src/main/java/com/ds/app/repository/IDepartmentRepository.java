package com.ds.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.app.entity.Department;

public interface IDepartmentRepository extends JpaRepository<Department, Long>{

}
