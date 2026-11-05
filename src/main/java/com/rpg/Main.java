package com.rpg;

import com.rpg.core.Consola;
import com.rpg.core.MotorJuego;

import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Consola consola = new Consola(scanner);
            MotorJuego motor = new MotorJuego(consola);
            ejecutarMenu(consola, motor);
        } catch (NoSuchElementException e) {
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    private static void ejecutarMenu(Consola consola, MotorJuego motor) {
        int opcion;
        do {
            mostrarMenu();
            // TALLER ARCHIVOS: aqui ofrecemos "Cargar partida", ver/borrar archivos con File, y ver logs
            opcion = consola.leerEntero("Opcion: ", 0, 7);
            switch (opcion) {
                case 1 -> motor.combateRapido();
                case 2 -> motor.jugarTorneo();
                case 3 -> motor.mostrarBestiario();
                case 4 -> motor.cargarPartida();
                case 5 -> motor.mostrarHistorialCombates();
                case 6 -> motor.mostrarInfoArchivos();
                case 7 -> motor.borrarPartida();
                default -> System.out.println("Hasta luego.");
            }
        } while (opcion != 0);
    }

    private static void mostrarMenu() {
        System.out.println("\n=== SIMULADOR DE BATALLA RPG ===");
        System.out.println("1. Combate rapido");
        System.out.println("2. Torneo");
        System.out.println("3. Ver bestiario");
        System.out.println("4. Cargar partida (.dat)");
        System.out.println("5. Ver historial de combates (.txt)");
        System.out.println("6. Informacion de archivos (File)");
        System.out.println("7. Borrar partida guardada");
        System.out.println("0. Salir");
    }
}
