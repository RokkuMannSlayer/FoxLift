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

    private static final String ARCHIVO = "estomago.txt";

    /**
     * Guarda un objeto detectado y devuelve
     * el ID asignado.
     */
    public synchronized int guardarObjeto(double distancia) throws Exception {

        int id = obtenerSiguienteId();

        LocalDate fecha = LocalDate.now();

        LocalTime hora = LocalTime.now();

        DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss");

        String fechaTexto = fecha.format(formatoFecha);

        String horaTexto = hora.format(formatoHora);

        try (FileWriter fw = new FileWriter(ARCHIVO, true); PrintWriter pw = new PrintWriter(fw)) {

            pw.printf("ID: %d | Fecha: %s | Hora: %s | Distancia: %.2f cm | Estado: OBJETO DETECTADO%n", id, fechaTexto, horaTexto, distancia);
        }

        return id;
    }

    /**
     * Busca el ID más alto del archivo
     * y devuelve el siguiente.
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

                    String parteId = linea.substring(3).split("\\|")[0].trim();

                    try {

                        int id = Integer.parseInt(parteId);

                        if (id > maxId) {

                            maxId = id;
                        }

                    } catch (NumberFormatException ignored) {
                    }
                }
            }

        } catch (Exception e) {

            System.out.println("Error leyendo " + ARCHIVO + ": " + e.getMessage());
        }

        return maxId + 1;
    }
}