package com.miapp.model;

/**
 * Entidad de dominio que representa a un enemigo del Bestiario.
 * POJO puro: atributos privados con getters, setters y validaciones.
 */
public class Monstruo {

    private int id;
    private String nombre;
    private int vida;
    private int ataque;

    public Monstruo() {
    }

    public Monstruo(String nombre, int vida, int ataque) {
        setNombre(nombre);
        setVida(vida);
        setAtaque(ataque);
    }

    public Monstruo(int id, String nombre, int vida, int ataque) {
        this(nombre, vida, ataque);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del monstruo no puede estar vacio.");
        }
        this.nombre = nombre.trim();
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        if (vida <= 0) {
            throw new IllegalArgumentException("La vida debe ser un numero positivo mayor a cero.");
        }
        this.vida = vida;
    }

    public int getAtaque() {
        return ataque;
    }

    public void setAtaque(int ataque) {
        if (ataque < 0) {
            throw new IllegalArgumentException("El ataque no puede ser negativo.");
        }
        this.ataque = ataque;
    }

    @Override
    public String toString() {
        return String.format("Monstruo [id=%d, nombre='%s', vida=%d, ataque=%d]", id, nombre, vida, ataque);
    }
}

