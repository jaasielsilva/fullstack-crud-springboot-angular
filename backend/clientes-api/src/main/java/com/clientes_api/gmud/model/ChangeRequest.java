package com.clientes_api.gmud.model;

import com.clientes_api.gmud.enums.ChangeStatus;
import com.clientes_api.gmud.enums.ChangeType;
import com.clientes_api.gmud.enums.DeployEnvironment;
import com.clientes_api.gmud.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "change_requests")
@Getter
@Setter
@NoArgsConstructor
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeType type = ChangeType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeStatus status = ChangeStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeployEnvironment environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel = RiskLevel.MEDIUM;

    @Column(columnDefinition = "TEXT")
    private String impactDescription;

    @Column(columnDefinition = "TEXT")
    private String rollbackPlan;

    private LocalDateTime deploymentWindowStart;
    private LocalDateTime deploymentWindowEnd;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime approvedAt;
    private LocalDateTime deployedAt;
    private LocalDateTime rolledBackAt;

    @Column(nullable = false)
    private String createdBy;

    private String version;
    private String artifact;

    @Column(unique = true)
    private String pipelineRunId;

    private String commitSha;
}
