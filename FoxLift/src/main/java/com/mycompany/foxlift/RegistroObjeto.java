package com.mycompany.foxlift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class RegistroObjeto {

    /**
     * Archivo persistente que representa
     * el "estómago" de la Orquita.
     */
    private static final String ARCHIVO = "estomago.txt";

    /**
     * Registra una captura en estomago.txt.
     *
     * El método es synchronized porque el archivo
     * es un recurso compartido entre diferentes hilos.
     */
    public synchronized int guardarObjeto(double distancia) throws Exception {

        int id = obtenerSiguienteId();

        LocalDate fecha = LocalDate.now();

        LocalTime hora = LocalTime.now();

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        String fechaTexto =
                fecha.format(
                        formatoFecha
                );

        String horaTexto = hora.format(formatoHora);

        /*
         * SECCIÓN CRÍTICA:
         *
         * La escritura sobre estomago.txt
         * está protegida por synchronized.
         */
        try (FileWriter fw = new FileWriter(ARCHIVO, true); PrintWriter pw = new PrintWriter(fw)) {

            pw.printf("ID: %d | Fecha: %s | Hora: %s | Distancia: %.2f cm | Estado: OBJETO DETECTADO%n", id, fechaTexto, horaTexto, distancia);
        }

        return id;
    }

    /**
     * Cuenta los registros válidos de captura
     * existentes actualmente en estomago.txt.
     *
     * También está synchronized porque el Auditor
     * puede consultar mientras otro hilo escribe.
     */
    public synchronized int contarRegistros() {

        File archivo = new File(ARCHIVO);

        if (!archivo.exists()) {

            return 0;
        }

        int cantidad = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String linea;

            while ((linea = br.readLine()) != null) {

                /*
                 * Consideramos válido un registro
                 * que contenga estos elementos.
                 */
                if (linea.startsWith("ID:") && linea.contains("Distancia:") && linea.contains("Estado: OBJETO DETECTADO")) {

                    cantidad++;
                }
            }

        } catch (Exception e) {

            System.out.println("Error consultando " + ARCHIVO + ": " + e.getMessage());
        }

        return cantidad;
    }

    /**
     * Obtiene el siguiente ID.
     *
     * Este método es llamado desde métodos
     * synchronized de esta misma clase.
     */
    private int obtenerSiguienteId() {

        File archivo = new File(ARCHIVO);

        if (!archivo.exists()) {

            return 1;
        }

        int maxId = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String linea;

            while ((linea = br.readLine())!= null) {

                if (linea.startsWith("ID:")) {

                    try {

                        String parteId = linea.substring(3).split("\\|")[0].trim();

                        int id = Integer.parseInt(parteId);

                        if (id > maxId) {

                            maxId = id;
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Error obteniendo ID: " + e.getMessage());
        }

        return maxId + 1;
    }
}
