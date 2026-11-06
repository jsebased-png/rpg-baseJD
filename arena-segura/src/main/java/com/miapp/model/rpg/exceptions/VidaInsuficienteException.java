package com.miapp.model.rpg.exceptions;

/** Excepcion no verificada: la habilidad costaria mas vida de la que se tiene. */
public class VidaInsuficienteException extends RuntimeException {

    public VidaInsuficienteException(String nombre, int costo, int vidaActual) {
        super(nombre + " no puede pagar " + costo + " de vida (le quedan " + vidaActual + ")");
    }
}
