package com.mycompany.foxlift;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.time.LocalDate;
import java.time.LocalTime;

import java.time.format.DateTimeFormatter;

/**
 * Gestiona el registro de objetos detectados.
 *
 * Los datos se almacenan en objetos.txt.
 */
public class RegistroObjeto {

    private static final String ARCHIVO = "estomago.txt";

    private int siguienteId;

    private final DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm:ss");

    public RegistroObjeto() {

        siguienteId = obtenerSiguienteId();
    }

    /**
     * Guarda un objeto detectado.
     *
     * @param distancia distancia medida
     * @return registro creado
     */
    public synchronized Registro guardarObjeto(double distancia) {

        int id = siguienteId++;

        LocalDate fecha = LocalDate.now();

        LocalTime hora = LocalTime.now();

        String fechaTexto = fecha.format(formatoFecha);

        String horaTexto = hora.format(formatoHora);

        String estado = "OBJETO DETECTADO";

        String linea = String.format("ID: %d | Fecha: %s | " + "Hora: %s | Distancia: %.2f cm " + "| Estado: %s", id, fechaTexto, horaTexto, distancia, estado);

        try (BufferedWriter escritor = new BufferedWriter(new FileWriter(ARCHIVO, true))) {

            escritor.write(linea);

            escritor.newLine();

        } catch (IOException e) {

            System.err.println("Error al guardar el objeto: " + e.getMessage());
        }

        return new Registro(id, fechaTexto, horaTexto, distancia, estado);
    }

    /**
     * Busca el último ID utilizado en objetos.txt.
     *
     * @return siguiente ID disponible
     */
    private int obtenerSiguienteId() {

        File archivo = new File(ARCHIVO);

        if (!archivo.exists()) {

            return 1;
        }

        int ultimoId = 0;

        try (BufferedReader lector = new BufferedReader(new FileReader(archivo))) {

            String linea;

            while ((linea = lector.readLine()) != null) {

                if (linea.startsWith("ID:")) {

                    String[] partes = linea.split("\\|");

                    if (partes.length > 0) {

                        String textoId = partes[0].replace("ID:", "").trim();

                        try {

                            int id = Integer.parseInt(textoId);

                            if (id > ultimoId) {

                                ultimoId = id;
                            }

                        } catch (NumberFormatException e) {

                            // Ignorar ID inválido
                        }
                    }
                }
            }

        } catch (IOException e) {

            System.err.println("Error leyendo estomago.txt: " + e.getMessage());
        }

        return ultimoId + 1;
    }

    /**
     * Representa un objeto detectado.
     */
    public static class Registro {

        private final int id;

        private final String fecha;

        private final String hora;

        private final double distancia;

        private final String estado;

        public Registro(int id, String fecha, String hora, double distancia, String estado) {

            this.id = id;

            this.fecha = fecha;

            this.hora = hora;

            this.distancia = distancia;

            this.estado = estado;
        }

        public int getId() {

            return id;
        }

        public String getFecha() {

            return fecha;
        }

        public String getHora() {

            return hora;
        }

        public double getDistancia() {

            return distancia;
        }

        public String getEstado() {

            return estado;
        }
    }
}