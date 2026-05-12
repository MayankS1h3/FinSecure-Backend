package com.ds.app.entity;
 
import com.ds.app.enums.ProjectStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDate;
import java.util.List;
 
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Project {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long projectId;
 
    @Column(nullable = false)
    private String projectName;
    
    private ProjectStatus status = ProjectStatus.ACTIVE;   // ACTIVE / COMPLETED / ON_HOLD
    private LocalDate startDate;
    private LocalDate endDate;          // null means ongoing
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnore
    private Company company;
 
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "department_id")
//    private Department department;
    
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Employee> assignedEmployees;;
 
    // convenience getters
    public Long getCompanyId()    { return company    != null ? company.getCompanyId()    : null; }
}