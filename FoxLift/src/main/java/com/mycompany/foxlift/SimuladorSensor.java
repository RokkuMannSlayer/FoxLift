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
     * Simula la distancia medida por el sensor.
     */
    public double medirDistancia() {

        /*
         * 60% de las veces:
         * objeto dentro del rango.
         *
         * 40%:
         * objeto fuera del rango.
         */

        if (random.nextDouble() < 0.60) {

            return 5.0
                    + random.nextDouble() * 15.0;

        } else {

            return 21.0
                    + random.nextDouble() * 79.0;
        }
    }

    /**
     * Genera las características físicas
     * del objeto detectado.
     */
    public CaracteristicasObjeto generarCaracteristicasObjeto() {

        /*
         * 60% de las veces simulamos una CANICA.
         */

        if (random.nextDouble() < 0.60) {

            double diametro =
                    14.0
                    + random.nextDouble() * 10.0;

            double peso =
                    3.0
                    + random.nextDouble() * 5.0;

            double redondez =
                    0.92
                    + random.nextDouble() * 0.08;

            double densidad =
                    0.90
                    + random.nextDouble() * 0.10;

            return new CaracteristicasObjeto(
                    diametro,
                    peso,
                    redondez,
                    densidad
            );

        } else {

            /*
             * Objeto que no debería ser
             * clasificado como canica.
             */

            double diametro =
                    5.0
                    + random.nextDouble() * 25.0;

            double peso =
                    1.0
                    + random.nextDouble() * 18.0;

            double redondez =
                    0.40
                    + random.nextDouble() * 0.45;

            double densidad =
                    0.20
                    + random.nextDouble() * 0.65;

            return new CaracteristicasObjeto(
                    diametro,
                    peso,
                    redondez,
                    densidad
            );
        }
    }

    /**
     * Envía la medición al servidor mediante TCP.
     */
    public void enviarMedicion() {

        double distancia =
                medirDistancia();

        CaracteristicasObjeto objeto =
                generarCaracteristicasObjeto();

        try (
                Socket socket =
                        new Socket(HOST, PUERTO);

                PrintWriter salida =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        )
        ) {

            String mensaje = String.format(Locale.US, "DISTANCIA:%.2f;" + "DIAMETRO:%.2f;" + "PESO:%.2f;" + "REDONDEZ:%.3f;" + "DENSIDAD:%.3f", distancia, objeto.getDiametro(), objeto.getPeso(), objeto.getRedondez(), objeto.getDensidad());

            System.out.println(
                    "Sensor enviando: "
                    + mensaje
            );

            salida.println(mensaje);

        } catch (Exception e) {

            System.out.println("Error del sensor: " + e.getMessage());
        }
    }

    public double getDistanciaDeteccion() {
        return DISTANCIA_DETECCION;
    }
}
