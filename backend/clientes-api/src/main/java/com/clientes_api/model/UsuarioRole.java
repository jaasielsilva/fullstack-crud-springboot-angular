package com.clientes_api.model;

public enum UsuarioRole {
    ADMIN("admin"),
    GERENTE("gerente"),
    VENDEDOR("vendedor"),
    SUPORTE("suporte"),
    USER("user");

    private String role;

    UsuarioRole(String role){
        this.role = role;
    }

    public String getRole(){
        return role;
    }
}
