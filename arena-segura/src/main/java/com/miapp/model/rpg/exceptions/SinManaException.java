package com.miapp.model.rpg.exceptions;

/** Se lanza cuando un hechizo cuesta mas mana del disponible. */
public class SinManaException extends JuegoException {

    public SinManaException(int requerido, int disponible) {
        super("Mana insuficiente: necesita " + requerido + " y tiene " + disponible);
    }
}
