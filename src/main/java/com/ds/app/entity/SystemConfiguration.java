package com.ds.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {
    @Id
    @Column(unique = true, nullable = false, updatable = false)
    private String configKey;

    @Column(nullable = false)
    private String configValue;
}
