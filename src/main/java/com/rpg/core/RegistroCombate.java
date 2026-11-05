package com.rpg.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Imprime cada evento del combate y lo guarda en memoria y a disco (.txt).
 * Implementa metodos para volcar el historial con BufferedWriter (append) y leerlo con BufferedReader.
 */
public class RegistroCombate {

    private final StringBuilder texto = new StringBuilder();

    public synchronized void agregar(String linea) {
        System.out.println(linea);
        texto.append(linea).append(System.lineSeparator());
    }

    public synchronized void agregar(String formato, Object... argumentos) {
        agregar(String.format(formato, argumentos));
    }

    public synchronized void limpiar() {
        texto.setLength(0);
    }

    public synchronized String getTexto() {
        return texto.toString();
    }

    /**
     * TALLER ARCHIVOS - Seccion 2:
     * Vuelca el log acumulado a un archivo .txt usando BufferedWriter + FileWriter en modo append.
     * Usa newLine() y try-with-resources.
     */
    public synchronized void volcarAArchivo(File archivo) throws IOException {
        File carpetaPadre = archivo.getParentFile();
        if (carpetaPadre != null && !carpetaPadre.exists()) {
            carpetaPadre.mkdirs();
        }
        // FileWriter(archivo, true) abre en modo append para conservar combates previos
        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, true))) {
            escritor.write(texto.toString());
            escritor.newLine();
            escritor.flush();
        }
    }

    /**
     * TALLER ARCHIVOS - Seccion 3:
     * Lee un archivo de log linea por linea con BufferedReader + FileReader.
     * Captura FileNotFoundException por separado y maneja excepciones utilmente.
     */
    public static String leerArchivoLog(File archivo) throws FileNotFoundException, IOException {
        if (!archivo.exists()) {
            throw new FileNotFoundException("El archivo de registro no existe: " + archivo.getName());
        }
        StringBuilder contenido = new StringBuilder();
        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                contenido.append(linea).append(System.lineSeparator());
            }
        }
        return contenido.toString();
    }
}
