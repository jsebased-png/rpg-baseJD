package com.rpg.entities;

import com.rpg.entities.enums.RaridadItem;
import java.io.Serializable;

/**
 * Representa el arma equipada por un personaje.
 * Implementa Serializable para permitir la persistencia del estado del Personaje.
 */
public class Arma implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String nombre;
    private final int danioBase;
    private final RaridadItem raridad;

    public Arma(String nombre, int danioBase, RaridadItem raridad) {
        if (danioBase < 0) {
            throw new IllegalArgumentException("El danio del arma no puede ser negativo");
        }
        this.nombre = nombre;
        this.danioBase = danioBase;
        this.raridad = raridad;
    }

    public double getDanioEfectivo() {
        return danioBase * raridad.getMultiplicadorStat();
    }

    public String getNombre() {
        return nombre;
    }

    public int getDanioBase() {
        return danioBase;
    }

    public RaridadItem getRaridad() {
        return raridad;
    }
}
