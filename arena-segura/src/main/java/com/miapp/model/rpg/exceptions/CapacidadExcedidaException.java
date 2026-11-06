package com.miapp.model.rpg.exceptions;

/** Se lanza al agregar un elemento a un inventario que ya esta lleno. */
public class CapacidadExcedidaException extends JuegoException {

    public CapacidadExcedidaException(int capacidad) {
        super("Inventario lleno (capacidad: " + capacidad + ")");
    }
}
