package com.mycompany.foxlift;

import javax.swing.SwingUtilities;

public class Foxlift {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            InterfazServidor servidor =
                    new InterfazServidor();

            servidor.setVisible(true);

            // Iniciar servidor TCP
            servidor.iniciarServidor();

            // Iniciar Auditor de manera independiente
            Auditor auditor =   new Auditor(servidor);

            auditor.iniciar();
        });
    }
}
