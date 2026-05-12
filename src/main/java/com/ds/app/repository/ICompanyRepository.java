package com.ds.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.app.entity.Company;

public interface ICompanyRepository extends JpaRepository<Company, Long>{

}
