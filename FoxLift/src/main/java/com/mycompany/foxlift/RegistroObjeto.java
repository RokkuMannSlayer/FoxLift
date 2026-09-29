package com.mycompany.foxlift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RegistroObjeto {

    private static final String ARCHIVO = "estomago.txt";

    /*
     * UN ÚNICO LOCK PARA TODO EL ARCHIVO.
     *
     * De esta forma, aunque existan varios objetos
     * RegistroObjeto, todos utilizan el mismo mecanismo
     * de sincronización.
     */
    private static final Object LOCK_ESTOMAGO = new Object();

    /**
     * Guarda una captura en estomago.txt.
     */
    public int guardarObjeto(double distancia, String clasificacion) throws Exception {

        synchronized (LOCK_ESTOMAGO) {

            /*
             * El ID se obtiene dentro de la sección crítica.
             * Esto evita IDs repetidos cuando dos hilos
             * intentan guardar al mismo tiempo.
             */
            int id = obtenerSiguienteId();

            LocalDate fecha = LocalDate.now();

            LocalTime hora = LocalTime.now();

            DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss");

            String fechaTexto = fecha.format(formatoFecha);

            String horaTexto = hora.format(formatoHora);

            try (FileWriter fw = new FileWriter(ARCHIVO, true); PrintWriter pw = new PrintWriter(fw)) {

                pw.printf(Locale.US, "ID: %d | Fecha: %s | Hora: %s | " + "Distancia: %.2f cm | " + "Estado: OBJETO DETECTADO | " + "Clasificacion: %s%n", id, fechaTexto, horaTexto, distancia, clasificacion);
            }

            return id;
        }
    }

    /**
     * Cuenta las capturas válidas almacenadas.
     */
    public int contarRegistros() {

        synchronized (LOCK_ESTOMAGO) {

            File archivo = new File(ARCHIVO);

            if (!archivo.exists()) {
                return 0;
            }

            int cantidad = 0;

            try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

                String linea;

                while ((linea = br.readLine())!= null) {

                    if (esRegistroValido(linea)) {
                        cantidad++;
                    }
                }

            } catch (Exception e) {

                System.out.println("Error consultando " + ARCHIVO + ": " + e.getMessage());
            }

            return cantidad;
        }
    }

    /**
     * Determina si una línea representa
     * una captura válida.
     */
    private boolean esRegistroValido(
            String linea) {

        if (linea == null) {
            return false;
        }

        return linea.startsWith("ID:") && linea.contains("Fecha:") && linea.contains("Hora:") && linea.contains("Distancia:") && linea.contains("Estado: OBJETO DETECTADO") && linea.contains("Clasificacion:");
    }

    /**
     * Obtiene el próximo ID disponible.
     *
     * Este método debe ejecutarse dentro
     * de LOCK_ESTOMAGO.
     */
    private int obtenerSiguienteId() {

        File archivo = new File(ARCHIVO);

        if (!archivo.exists()) {
            return 1;
        }

        int maxId = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String linea;

            while ((linea = br.readLine()) != null) {

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