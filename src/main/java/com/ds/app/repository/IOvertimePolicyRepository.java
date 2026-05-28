package com.ds.app.repository;

import com.ds.app.entity.OvertimePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IOvertimePolicyRepository extends JpaRepository<OvertimePolicy, Long> {

    // Fetch a specific policy by its exact unique constraint combination
    Optional<OvertimePolicy> findByCountryAndStateAndName(String country, String state, String name);

    // Fetch all policies available in a specific state (Useful for HR Admin dropdowns)
    List<OvertimePolicy> findByCountryAndState(String country, String state);

    // Fetch all policies available in a specific country
    List<OvertimePolicy> findByCountry(String country);

    // Fetch all policies available in a specific state
    List<OvertimePolicy> findByState(String state);

    // Check if a policy name already exists in a given state (Useful for validation when creating a new one)
    boolean existsByCountryAndStateAndName(String country, String state, String name);
}