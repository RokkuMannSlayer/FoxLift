package com.mycompany.foxlift;

public class Foxlift {

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(() -> {

            InterfazServidor servidor = new InterfazServidor();

            servidor.setVisible(true);

            // Iniciar el servidor TCP en el puerto 5000
            servidor.iniciarServidor();
        });
    }
}