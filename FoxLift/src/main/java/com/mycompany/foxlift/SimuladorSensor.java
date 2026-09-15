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
     * El 60% de las veces genera una distancia
     * entre 5 y 20 cm.
     *
     * El 40% restante genera una distancia
     * entre 21 y 100 cm.
     */
    public double medirDistancia() {

        if (random.nextDouble() < 0.60) {

            return 5.0 + random.nextDouble() * 15.0;

        } else {

            return 21.0 + random.nextDouble() * 79.0;
        }
    }

    /**
     * Envía una medición al servidor mediante TCP.
     */
    public void enviarMedicion() {

        double distancia = medirDistancia();

        try (Socket socket = new Socket(HOST, PUERTO);

                PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)
        ) {

            /*
             * Locale.US es importante para que el número
             * se envíe con punto:
             *
             * DISTANCIA:13.72
             *
             * y no:
             *
             * DISTANCIA:13,72
             */
            String mensaje = String.format(Locale.US, "DISTANCIA:%.2f", distancia);

            System.out.println("Enviando al servidor: "+ mensaje);

            salida.println(mensaje);

        } catch (Exception e) {

            System.out.println("Error conectando con el servidor: " + e.getMessage());
        }
    }

    public double getDistanciaDeteccion() {

        return DISTANCIA_DETECCION;
    }
}