package com.mycompany.foxlift;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;

/**
 * Simula el Arduino y su sensor ultrasónico.
 *
 * Genera una distancia y la envía al servidor
 * mediante un Socket TCP.
 */
public class SimuladorSensor {

    private static final String HOST = "localhost";

    private static final int PUERTO = 5000;

    private static final double DISTANCIA_DETECCION = 20.0;

    private final Random random;

    public SimuladorSensor() {

        random = new Random();
    }

    /**
     * Simula una medición del sensor.
     *
     * @return distancia simulada en centímetros
     */
    public double medirDistancia() {

        if (random.nextDouble() < 0.60) {

            // Objeto entre 5 y 20 cm
            return 5.0 + random.nextDouble() * 15.0;

        } else {

            // Sin objeto entre 21 y 100 cm
            return 21.0 + random.nextDouble() * 79.0;
        }
    }

    /**
     * Envía una medición al servidor TCP.
     */
    public void enviarMedicion() {

        double distancia = medirDistancia();

        try (
                Socket socket = new Socket(HOST, PUERTO);

                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

            /*
             * IMPORTANTE:
             *
             * Se utiliza Locale.US para garantizar
             * que el decimal se envíe con PUNTO.
             *
             * Ejemplo:
             *
             * DISTANCIA:13.72
             *
             * y no:
             *
             * DISTANCIA:13,72
             */

            String mensaje = String.format(Locale.US, "DISTANCIA:%.2f", distancia);

            System.out.println("Enviando al servidor: " + mensaje);

            salida.println(mensaje);

        } catch (Exception e) {

            System.err.println("No se pudo conectar con el servidor TCP.");

            System.err.println("Verifique que FoxLift esté ejecutándose.");

            System.err.println("Error: " + e.getMessage());
        }
    }

    /**
     * Devuelve el límite de detección.
     *
     * @return distancia máxima de detección
     */
    public double getDistanciaDeteccion() {

        return DISTANCIA_DETECCION;
    }
}
