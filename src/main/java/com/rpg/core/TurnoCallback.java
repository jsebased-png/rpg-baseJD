package com.rpg.core;

/**
 * Notifica cuando el temporizador de turno de un combatiente se ha agotado.
 */
@FunctionalInterface
public interface TurnoCallback {

    /**
     * Invocado cuando el tiempo para elegir una accion en el turno expira.
     */
    void onTiempoAgotado();
}

