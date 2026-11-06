package com.miapp.view;

import java.util.Scanner;

/**
 * Utilidad de entrada por consola con validacion para la capa View.
 * No contiene logica de negocio ni sentencias SQL.
 */
public class ConsolaUtil {

    private static final Scanner scanner = new Scanner(System.in);

    public static String leerTexto(String mensaje) {
        System.out.print(mensaje);
        String linea = scanner.nextLine().trim();
        if (linea.startsWith("\uFEFF")) {
            linea = linea.substring(1).trim();
        }
        return linea;
    }

    public static int leerEntero(String mensaje, int minimo, int maximo) {
        while (true) {
            String input = leerTexto(mensaje);
            if (input.isEmpty()) {
                System.out.println("No ingresaste ningun valor.");
                continue;
            }
            try {
                int valor = Integer.parseInt(input);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                System.out.printf("Numero fuera de rango (%d a %d).%n", minimo, maximo);
            } catch (NumberFormatException e) {
                System.out.println("'" + input + "' no es un numero valido.");
            }
        }
    }

    public static void pausar() {
        System.out.print("\nPresiona [Enter] para continuar...");
        scanner.nextLine();
    }
}

