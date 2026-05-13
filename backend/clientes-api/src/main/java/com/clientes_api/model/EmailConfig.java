package com.clientes_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(
        name = "email_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_configs_tenant", columnNames = "tenant_id")
)
@Entity(name = "EmailConfig")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    private String host;
    private Integer port;
    private String usuario;
    private String senha; // Token do Gmail
    private String protocol = "smtp";
    private Boolean auth = true;
    private Boolean starttls = true;

    public EmailConfig(Long tenantId, String host, Integer port, String usuario, String senha) {
        this.tenantId = tenantId;
        this.host = host;
        this.port = port;
        this.usuario = usuario;
        this.senha = senha;
    }
}
