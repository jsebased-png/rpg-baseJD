package com.miapp.model;

/**
 * Data Transfer Object (DTO) para representar la tabla de clasificacion (Ranking General).
 * Agrupa el total de victorias por cada jugador ordenado descendentemente.
 */
public class RankingDTO {

    private int posicion;
    private String usuario;
    private String rol;
    private int totalVictorias;

    public RankingDTO() {
    }

    public RankingDTO(int posicion, String usuario, String rol, int totalVictorias) {
        this.posicion = posicion;
        this.usuario = usuario;
        this.rol = rol;
        this.totalVictorias = totalVictorias;
    }

    public int getPosicion() {
        return posicion;
    }

    public void setPosicion(int posicion) {
        this.posicion = posicion;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public int getTotalVictorias() {
        return totalVictorias;
    }

    public void setTotalVictorias(int totalVictorias) {
        this.totalVictorias = totalVictorias;
    }

    @Override
    public String toString() {
        return String.format("#%02d | %-15s (%-7s) | %3d victorias", posicion, usuario, rol, totalVictorias);
    }
}

