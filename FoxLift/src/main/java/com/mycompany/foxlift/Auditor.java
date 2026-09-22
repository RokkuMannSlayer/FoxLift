package com.mycompany.foxlift;

/**
 * Auditor independiente encargado de consultar
 * periódicamente el estado persistente de la Orquita.
 */
public class Auditor implements Runnable {

    private static final int REGISTROS_ESCAPE = 3;

    private static final long INTERVALO_AUDITORIA = 2000;

    private final RegistroObjeto registro;

    private final InterfazServidor interfaz;

    private Thread hiloAuditor;

    private boolean ejecutando;

    private boolean escapeInformado;

    /**
     * Constructor.
     */
    public Auditor(InterfazServidor interfaz) {

        this.interfaz = interfaz;

        this.registro = new RegistroObjeto();

        this.ejecutando = false;

        this.escapeInformado = false;
    }

    /**
     * Inicia el hilo del Auditor.
     */
    public void iniciar() {

        if (ejecutando) {

            return;
        }

        ejecutando = true;

        hiloAuditor = new Thread(this, "Auditor-Orquita");

        hiloAuditor.start();

        interfaz.agregarLog("Auditor iniciado.");

        interfaz.agregarLog("El Auditor consultará estomago.txt periódicamente.");
    }

    /**
     * Ejecución independiente del Auditor.
     */
    @Override
    public void run() {

        while (ejecutando) {

            try {

                /*
                 * Consulta sincronizada del archivo.
                 */
                int cantidad = registro.contarRegistros();

                interfaz.agregarLog("Auditor: " + cantidad + " registros válidos encontrados.");

                /*
                 * Cuando existen 3 registros,
                 * se habilita el Modo Escape.
                 */
                if (
                        cantidad >= REGISTROS_ESCAPE
                        && !escapeInformado
                ) {

                    escapeInformado = true;

                    interfaz.modoEscape();
                }

                Thread.sleep(INTERVALO_AUDITORIA);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                ejecutando = false;

            } catch (Exception e) {

                interfaz.agregarLog("Error del Auditor: " + e.getMessage());
            }
        }
    }

    /**
     * Detiene el Auditor.
     */
    public void detener() {

        ejecutando = false;

        if (hiloAuditor != null && hiloAuditor.isAlive()) {

            hiloAuditor.interrupt();
        }
    }
}
