package com.miapp.model;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa la victoria de un jugador frente a un monstruo.
 * Asocia jugador_id, monstruo_id y fecha/hora del combate.
 */
public class Victoria {

    private int id;
    private int jugadorId;
    private int monstruoId;
    private LocalDateTime fecha;

    public Victoria() {
    }

    public Victoria(int jugadorId, int monstruoId) {
        this.jugadorId = jugadorId;
        this.monstruoId = monstruoId;
        this.fecha = LocalDateTime.now();
    }

    public Victoria(int id, int jugadorId, int monstruoId, LocalDateTime fecha) {
        this.id = id;
        this.jugadorId = jugadorId;
        this.monstruoId = monstruoId;
        this.fecha = fecha;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJugadorId() {
        return jugadorId;
    }

    public void setJugadorId(int jugadorId) {
        this.jugadorId = jugadorId;
    }

    public int getMonstruoId() {
        return monstruoId;
    }

    public void setMonstruoId(int monstruoId) {
        this.monstruoId = monstruoId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    @Override
    public String toString() {
        return String.format("Victoria [id=%d, jugadorId=%d, monstruoId=%d, fecha=%s]",
                id, jugadorId, monstruoId, fecha);
    }
}

