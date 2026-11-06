package com.miapp.model;

import java.time.LocalDateTime;

/**
 * DTO que proyecta la informacion detallada de una victoria individual
 * para la visualizacion del historial del jugador.
 */
public class HistorialVictoriaDTO {

    private int victoriaId;
    private String usuario;
    private String nombreMonstruo;
    private int vidaMonstruo;
    private int ataqueMonstruo;
    private LocalDateTime fecha;

    public HistorialVictoriaDTO() {
    }

    public HistorialVictoriaDTO(int victoriaId, String usuario, String nombreMonstruo,
                                int vidaMonstruo, int ataqueMonstruo, LocalDateTime fecha) {
        this.victoriaId = victoriaId;
        this.usuario = usuario;
        this.nombreMonstruo = nombreMonstruo;
        this.vidaMonstruo = vidaMonstruo;
        this.ataqueMonstruo = ataqueMonstruo;
        this.fecha = fecha;
    }

    public int getVictoriaId() {
        return victoriaId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombreMonstruo() {
        return nombreMonstruo;
    }

    public int getVidaMonstruo() {
        return vidaMonstruo;
    }

    public int getAtaqueMonstruo() {
        return ataqueMonstruo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    @Override
    public String toString() {
        return String.format("[%s] Vencio a '%s' (HP: %d, ATK: %d)",
                fecha != null ? fecha.toString().replace('T', ' ') : "N/D",
                nombreMonstruo, vidaMonstruo, ataqueMonstruo);
    }
}

