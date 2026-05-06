package com.ds.app.repository;

import com.ds.app.entity.SystemConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ISystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {
}
