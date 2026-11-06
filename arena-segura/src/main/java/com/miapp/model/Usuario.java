package com.miapp.model;

/**
 * Entidad de dominio que representa a un usuario en el sistema.
 * POJO puro: sin dependencias de JDBC ni detalles de persistencia.
 */
public class Usuario {

    public static final String ROL_JUGADOR = "JUGADOR";
    public static final String ROL_ADMIN = "ADMIN";

    private int id;
    private String usuario;
    private String clave;
    private String rol;

    public Usuario() {
    }

    public Usuario(String usuario, String clave, String rol) {
        this.usuario = usuario;
        this.clave = clave;
        this.rol = rol != null ? rol.toUpperCase() : ROL_JUGADOR;
    }

    public Usuario(int id, String usuario, String clave, String rol) {
        this(usuario, clave, rol);
        this.id = id;
    }

    public boolean esAdmin() {
        return ROL_ADMIN.equalsIgnoreCase(this.rol);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getClave() {
        return clave;
    }

    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol != null ? rol.toUpperCase() : ROL_JUGADOR;
    }

    @Override
    public String toString() {
        return String.format("Usuario [id=%d, usuario='%s', rol='%s']", id, usuario, rol);
    }
}

