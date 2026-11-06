package com.miapp.model.rpg.exceptions;

/** Base de las excepciones verificadas del dominio del juego. */
public abstract class JuegoException extends Exception {

    protected JuegoException(String mensaje) {
        super(mensaje);
    }
}
