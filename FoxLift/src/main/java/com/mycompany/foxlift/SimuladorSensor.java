package com.mycompany.foxlift;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;
import java.util.Random;

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
     * Algunas mediciones están dentro del rango
     * de detección y otras fuera.
     */
    public double medirDistancia() {

        if (random.nextDouble() < 0.60) {

            // Entre 5 y 20 cm
            return 5.0 + random.nextDouble() * 15.0;

        } else {

            // Entre 21 y 100 cm
            return 21.0 + random.nextDouble() * 79.0;
        }
    }

    /**
     * Envía la medición al servidor mediante TCP.
     */
    public void enviarMedicion() {

        double distancia = medirDistancia();

        try (Socket socket = new Socket(HOST, PUERTO); PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

            /*
             * Locale.US obliga a utilizar punto decimal.
             *
             * Correcto:
             * DISTANCIA:13.72
             *
             * Incorrecto:
             * DISTANCIA:13,72
             */
            String mensaje = String.format(Locale.US, "DISTANCIA:%.2f", distancia);

            System.out.println("Sensor enviando: " + mensaje);

            salida.println(mensaje);

        } catch (Exception e) {

            System.out.println("Error del sensor: " + e.getMessage());
        }
    }

    public double getDistanciaDeteccion() {

        return DISTANCIA_DETECCION;
    }
}
