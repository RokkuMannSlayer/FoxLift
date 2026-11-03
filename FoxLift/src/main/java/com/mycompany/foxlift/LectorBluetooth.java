package com.mycompany.foxlift;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class LectorBluetooth {

    private SerialPort puerto;

    public void iniciar(String nombrePuerto) {
        puerto = SerialPort.getCommPort(nombrePuerto);
        puerto.setBaudRate(9600);
        puerto.setNumDataBits(8);
        puerto.setNumStopBits(SerialPort.ONE_STOP_BIT);
        puerto.setParity(SerialPort.NO_PARITY);
        puerto.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            0, 0
        );

        if (!puerto.openPort()) {
            System.out.println(
                "No se pudo abrir el puerto " + nombrePuerto
            );
            return;
        }

        System.out.println(
            "Bluetooth conectado en " + nombrePuerto
        );

        Thread lector = new Thread(() -> {
            try (BufferedReader entrada =
                new BufferedReader(new InputStreamReader(
                    puerto.getInputStream(),
                    StandardCharsets.UTF_8
                ))) {

                String linea;

                while ((linea = entrada.readLine()) != null) {
                    procesarMensaje(linea.trim());
                }

            } catch (Exception e) {
                System.err.println(
                    "Error leyendo Bluetooth: " + e.getMessage()
                );
            }
        });

        lector.setDaemon(true);
        lector.start();
    }

    private void procesarMensaje(String mensaje) {
        if (!mensaje.startsWith("OBJETO:")) {
            return;
        }

        try {
            double distancia = Double.parseDouble(mensaje.substring("OBJETO:".length()).trim());

            System.out.printf("Objeto detectado a %.2f cm. " + "Clasificación pendiente.%n", distancia);

            // No registrar como canica todavía.
            // La distancia por sí sola no permite
            // distinguir una canica de otros objetos.

        } catch (NumberFormatException e) {
            System.err.println("Mensaje de distancia inválido: " + mensaje);
        }
    }

    public void cerrar() {
        if (puerto != null && puerto.isOpen()) {
            puerto.closePort();
        }
    }
}