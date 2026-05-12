package com.ds.app.entity;
 
import com.ds.app.enums.CompanyType;
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
public class Company {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;
 
    @Column(nullable = false, unique = true)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type;
    
    private String code;                        // unique e.g. ICICI-001
    
    @Builder.Default
    private Boolean restrictsInvestment = false;
    
    private boolean isActive;        
 
    // bidirectional — @JsonIgnore prevents loop when serialising Company
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Department> departments;
 
    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Project> projects;
 
//    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
//    @JsonIgnore
//    private List<Employee> employees = new ArrayList<>();
}