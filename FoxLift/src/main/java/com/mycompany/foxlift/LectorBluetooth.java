
package com.mycompany.foxlift;

import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class LectorBluetooth {

    private SerialPort puerto;
    private Thread hiloLectura;
    private volatile boolean conectado;

    // Buscar los puertos seriales disponibles.
    public static List<String> detectarPuertos() {
        List<String> puertos = new ArrayList<>();

        for (SerialPort p : SerialPort.getCommPorts()) {
            puertos.add(
                p.getSystemPortName() + " - " +
                p.getDescriptivePortName()
            );
        }

        return puertos;
    }

    // Conectar con el módulo Bluetooth.
    public boolean iniciar(String nombrePuerto, Consumer<String> receptor) {

        if (conectado) {
            return true;
        }

        puerto = SerialPort.getCommPort(nombrePuerto);

        puerto.setComPortParameters(9600, 8, 1, 0);
        puerto.setComPortTimeouts(
            SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
            0,
            0
        );

        if (!puerto.openPort()) {
            puerto = null;
            return false;
        }

        conectado = true;

        hiloLectura = new Thread(() -> {
            try (BufferedReader entrada = new BufferedReader(
                    new InputStreamReader(
                        puerto.getInputStream(),
                        StandardCharsets.UTF_8
                    ))) {

                String mensaje;

                while (conectado && (mensaje = entrada.readLine()) != null) {
                    if (receptor != null) {
                        receptor.accept(mensaje.trim());
                    }
                }

            } catch (Exception e) {
                if (conectado && receptor != null) {
                    receptor.accept("ERROR BLUETOOTH: " + e.getMessage());
                }
            } finally {
                conectado = false;
                if (puerto != null && puerto.isOpen()) {
                    puerto.closePort();
                }
            }
        }, "Lector-Bluetooth");

        hiloLectura.setDaemon(true);
        hiloLectura.start();

        return true;
    }

    // Enviar un comando al Arduino.
    public synchronized boolean enviar(String comando) {

        if (!conectado || puerto == null || !puerto.isOpen()) {
            return false;
        }

        byte[] datos = (comando + "\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        int enviados = puerto.writeBytes(datos, datos.length);

        System.out.println("Comando: " + comando);
        System.out.println("Bytes enviados: " + enviados);

        return enviados == datos.length;
    }

    public boolean estaConectado() {
        return conectado;
    }

    // Desconectar el Bluetooth.
    public synchronized void detener() {

        conectado = false;

        if (puerto != null && puerto.isOpen()) {
            puerto.closePort();
        }

        puerto = null;
    }
}
