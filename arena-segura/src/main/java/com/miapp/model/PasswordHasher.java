package com.miapp.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilidad de seguridad para el hash de contrasenas mediante el algoritmo SHA-256.
 *
 * NOTA DE SEGURIDAD ARQUITECTONICA:
 * Aunque las contrasenas se almacenen y comparen como hashes SHA-256 hexadecimales,
 * el uso de PreparedStatement en la capa DAO sigue siendo 100 % OBLIGATORIO.
 *
 * Justificacion:
 * 1. La inyeccion SQL puede atacar cualquier parametro (por ejemplo el campo 'usuario'),
 *    anulando la evaluacion de la contrasena mediante operadores logicos (OR 1=1 --).
 * 2. Un atacante podria inyectar sentencias DROP, UNION o UPDATE si la consulta se concatena.
 * 3. PreparedStatement separa estrictamente la estructura SQL del arbol de ejecucion compilado
 *    de los literales de datos, neutralizando la inyeccion con independencia del hashing.
 */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    /**
     * Calcula el resumen criptografico SHA-256 de una cadena y lo retorna en formato hexadecimal.
     *
     * @param entrada Texto en claro de la contrasena
     * @return Cadena hexadecimal de 64 caracteres representativa del hash
     */
    public static String hash(String entrada) {
        if (entrada == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(entrada.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM actual.", e);
        }
    }
}

