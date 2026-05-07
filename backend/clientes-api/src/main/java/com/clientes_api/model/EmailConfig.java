package com.clientes_api.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table(name = "email_configs")
@Entity(name = "EmailConfig")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String host;
    private Integer port;
    private String usuario;
    private String senha; // Token do Gmail
    private String protocol = "smtp";
    private Boolean auth = true;
    private Boolean starttls = true;

    public EmailConfig(String host, Integer port, String usuario, String senha) {
        this.host = host;
        this.port = port;
        this.usuario = usuario;
        this.senha = senha;
    }
}
