package com.rpg.persistencia;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Gestor integral de persistencia para el Simulador de Batalla RPG.
 * Maneja archivos planos (.txt) con BufferedWriter/BufferedReader y
 * archivos binarios (.dat) mediante serializacion con ObjectOutputStream/ObjectInputStream.
 * Sincronizado para acceso concurrente seguro con AutoGuardadoDaemon.
 *
 * INVESTIGACION OBLIGATORIA (TALLER ARCHIVOS):
 *
 * 1. FileWriter vs Files.writeString:
 *    - FileWriter (combinado con BufferedWriter): Trabaja como un flujo orientado a caracteres basado
 *      en buffer. Permite la escritura progresiva por fragmentos (chunks) sin requerir que la totalidad
 *      de los datos este en memoria, y admite directamente el modo de anexion ("append" = true).
 *      Debe gestionarse su ciclo de vida cerrando el flujo (idealmente con try-with-resources).
 *      Se usa principalmente para archivos de log o registros continuos que crecen con el tiempo.
 *    - Files.writeString (NIO.2, Java 11+): Es un metodo estatico de conveniencia que toma una cadena
 *      completa y la escribe a disco en una sola invocacion atomica de alto nivel, abriendo y cerrando
 *      el canal de E/S internamente. Se usa para escribir de una sola vez documentos, configuraciones
 *      o datos pequenos/medianos donde el contenido ya se encuentra completo en memoria y se busca
 *      maxima concision y rapidez de desarrollo.
 *
 * 2. Seguridad de Serializable y Deserializacion Insegura:
 *    - Deserializar datos (ObjectInputStream.readObject()) provenientes de orígenes no confiables o manipulables
 *      representa un grave riesgo de ejecucion remota de codigo (RCE) o denegacion de servicio (DoS).
 *      Un atacante puede alterar la carga util binaria creando una "gadget chain" (secuencias de clases
 *      presentes en el classpath que aprovechan metodos magicos como readObject o finalize para ejecutar comandos).
 *    - Alternativas seguras recomendadas: Utilizar formatos de intercambio estructurados y declarativos
 *      como JSON o YAML con esquemas restringidos, o buffers de serializacion binaria fuertemente tipados
 *      como Protocol Buffers (Protobuf) o FlatBuffers, los cuales deserializan unicamente datos y no instancian
 *      comportamientos de clases arbitrarias.
 */
public class GestorPersistencia {

    private static final Object LOCK_PERSISTENCIA = new Object();
    public static final String CARPETA_PARTIDAS = "partidas";
    public static final String ARCHIVO_LOG_RELATIVO = CARPETA_PARTIDAS + File.separator + "registro_combates.txt";
    public static final String ARCHIVO_DAT_RELATIVO = CARPETA_PARTIDAS + File.separator + "partida_rpg.dat";

    /**
     * TALLER ARCHIVOS - Seccion 1: Clase File
     * Asegura la existencia de la carpeta contenedora con mkdirs().
     */
    public static void asegurarDirectorio(File directorio) {
        if (!directorio.exists()) {
            boolean creado = directorio.mkdirs();
            if (creado) {
                System.out.println("[Archivo] Directorio creado exitosamente: " + directorio.getPath());
            }
        }
    }

    /**
     * Obtiene una instancia de File basada en una ruta relativa.
     */
    public static File obtenerArchivoRelativo(String rutaRelativa) {
        return new File(rutaRelativa);
    }

    /**
     * Obtiene una instancia de File con su ruta absoluta y la imprime.
     * Explica la diferencia: la ruta relativa se resuelve a partir del directorio de trabajo actual
     * del proceso en ejecucion (user.dir), mientras que la ruta absoluta identifica de forma unica e inequivoca
     * la ubicacion en el sistema de archivos desde la raiz del disco.
     */
    public static File obtenerArchivoAbsoluto(String ruta) {
        File file = new File(ruta);
        return file.getAbsoluteFile();
    }

    /**
     * Muestra informacion detallada del archivo usando File.exists(), length(), getName() y getAbsolutePath().
     */
    public static String describirArchivo(File archivo) {
        if (archivo == null) {
            return "Archivo nulo.";
        }
        asegurarDirectorio(archivo.getParentFile() != null ? archivo.getParentFile() : new File("."));
        return String.format("Nombre: %s | Existe: %b | Tamano: %d bytes | Ruta absoluta: %s",
                archivo.getName(), archivo.exists(), archivo.length(), archivo.getAbsolutePath());
    }

    /**
     * Crea un archivo si no existe usando File.createNewFile(), gestionando IOException.
     */
    public static boolean crearArchivoSiNoExiste(File archivo) {
        asegurarDirectorio(archivo.getParentFile() != null ? archivo.getParentFile() : new File("."));
        try {
            return archivo.createNewFile();
        } catch (IOException e) {
            System.err.println("No se pudo crear el archivo " + archivo.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * TALLER ARCHIVOS - Seccion 2: Escritura (BufferedWriter + FileWriter)
     * Vuelca texto en modo anexar (append = true), asegurando salto con newLine() y flush().
     * Sincronizado para proteger contra escrituras simultaneas del daemon.
     */
    public static void agregarLogTexto(File archivo, String contenido) {
        synchronized (LOCK_PERSISTENCIA) {
            asegurarDirectorio(archivo.getParentFile() != null ? archivo.getParentFile() : new File("."));
            try (BufferedWriter escritor = new BufferedWriter(new FileWriter(archivo, true))) {
                escritor.write(contenido);
                escritor.newLine();
                escritor.flush();
            } catch (IOException e) {
                System.err.println("Error al escribir el log en disco: " + e.getMessage());
            }
        }
    }

    /**
     * TALLER ARCHIVOS - Seccion 3: Lectura (BufferedReader + FileReader)
     * Lee linea por linea con while ((linea = lector.readLine()) != null).
     * Captura FileNotFoundException por separado.
     */
    public static String leerLogTexto(File archivo) {
        synchronized (LOCK_PERSISTENCIA) {
            if (!archivo.exists()) {
                System.out.println("El archivo no existe aun: " + archivo.getName());
                return "";
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {
                String linea;
                while ((linea = lector.readLine()) != null) {
                    sb.append(linea).append(System.lineSeparator());
                }
            } catch (FileNotFoundException e) {
                System.err.println("Archivo no encontrado: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error al leer el archivo de log: " + e.getMessage());
            }
            return sb.toString();
        }
    }

    /**
     * TALLER ARCHIVOS - Seccion 4: Borrado
     * Borrado con File.delete() revisando el boolean retornado.
     */
    public static boolean borrarConFile(File archivo) {
        synchronized (LOCK_PERSISTENCIA) {
            if (!archivo.exists()) {
                System.out.println("El archivo a borrar no existe: " + archivo.getName());
                return false;
            }
            boolean borrado = archivo.delete();
            if (borrado) {
                System.out.println("Archivo borrado exitosamente con File.delete(): " + archivo.getName());
            } else {
                System.err.println("No se pudo borrar el archivo con File.delete(): " + archivo.getName());
            }
            return borrado;
        }
    }

    /**
     * Borrado con Files.deleteIfExists(Path).
     * Diferencia con File.delete():
     * File.delete() retorna simplemente un boolean false si falla o si el archivo no existe, sin dar causa.
     * Files.deleteIfExists(Path) de NIO retorna boolean indicando si existia y fue borrado, pero ante fallos
     * reales del sistema (permisos, archivo bloqueado) lanza excepciones explicitas como DirectoryNotEmptyException
     * o AccessDeniedException o IOException detallando el motivo exacto del fallo.
     */
    public static boolean borrarConNIO(Path ruta) {
        synchronized (LOCK_PERSISTENCIA) {
            try {
                boolean eliminado = Files.deleteIfExists(ruta);
                if (eliminado) {
                    System.out.println("Archivo borrado exitosamente con Files.deleteIfExists(): " + ruta.getFileName());
                } else {
                    System.out.println("El archivo no existia para ser borrado.");
                }
                return eliminado;
            } catch (IOException e) {
                System.err.println("Excepcion al borrar archivo con Files.deleteIfExists: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * TALLER ARCHIVOS - Seccion 5 & 6 & 7: Serializacion con try-with-resources y proteccion concurrente.
     * Guarda el estado de la partida en formato binario (.dat) usando ObjectOutputStream.
     */
    public static boolean guardarPartida(EstadoPartida estado, File destino) {
        synchronized (LOCK_PERSISTENCIA) {
            asegurarDirectorio(destino.getParentFile() != null ? destino.getParentFile() : new File("."));
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(destino))) {
                oos.writeObject(estado);
                oos.flush();
                return true;
            } catch (NotSerializableException e) {
                System.err.println("Error critico de serializacion: clase no serializable: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error de E/S al guardar la partida: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Carga el estado de la partida desde disco con ObjectInputStream y casteo de tipos.
     * Maneja FileNotFoundException, ClassNotFoundException, NotSerializableException e IOException.
     */
    public static EstadoPartida cargarPartida(File origen) {
        synchronized (LOCK_PERSISTENCIA) {
            if (!origen.exists()) {
                System.out.println("No se encontro el archivo de guardado: " + origen.getName());
                return null;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(origen))) {
                Object obj = ois.readObject();
                if (obj instanceof EstadoPartida estado) {
                    System.out.println("Partida cargada exitosamente desde disco (" + origen.getName() + ").");
                    return estado;
                } else {
                    System.err.println("El archivo no contiene un estado de partida valido.");
                }
            } catch (FileNotFoundException e) {
                System.err.println("Archivo de partida no encontrado: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("Error de compatibilidad: clase no encontrada al cargar: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error de E/S al leer la partida: " + e.getMessage());
            }
            return null;
        }
    }
}

