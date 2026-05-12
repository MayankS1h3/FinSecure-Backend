package com.ds.app.entity;
 
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.util.List;
 
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deptId;
 
    @Column(nullable = false)
    private String name;
    
    private String code;
    
    @Builder.Default
    private Boolean isActive = true;
 
    // Department belongs to ONE Company — @ManyToOne (consistent with Project)
    // @JsonIgnoreProperties stops loop: Dept → Company.departments → Dept → ...
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    @JsonIgnore
    private Company company;
 
    // bidirectional 
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Employee> employees;
 
//    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
//    @JsonIgnore
//    private List<Project> projects = new ArrayList<>();
 
    // convenience getter — other code can still call dept.getCompanyId()
    public Long getCompanyId() {
        return company != null ? company.getCompanyId() : null;
    }
}