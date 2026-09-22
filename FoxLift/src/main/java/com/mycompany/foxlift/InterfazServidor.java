package com.mycompany.foxlift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class InterfazServidor extends JFrame {

    // ==========================================
    // CONFIGURACIÓN
    // ==========================================

    private static final int PUERTO = 5000;

    private static final double LIMITE_DETECCION = 20.0;

    // ==========================================
    // SERVIDOR
    // ==========================================

    private ServerSocket servidorSocket;

    private Socket clienteSocket;

    private Thread hiloServidor;

    // ==========================================
    // OBJETOS
    // ==========================================

    private final SimuladorSensor simulador;

    private final RegistroObjeto registro;

    // ==========================================
    // COMPONENTES
    // ==========================================

    private JLabel lblEstadoServidor;

    private JLabel lblEstadoSensor;

    private JLabel lblDistancia;

    private JLabel lblTotal;

    private JTable tablaObjetos;

    private DefaultTableModel modeloTabla;

    private JTextArea areaLog;

    private JButton btnSimular;

    private JButton btnLimpiar;

    // ==========================================
    // ESTADO DEL MODO ESCAPE
    // ==========================================

    private boolean modoEscapeActivo = false;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public InterfazServidor() {

        simulador = new SimuladorSensor();

        registro = new RegistroObjeto();

        configurarVentana();

        crearInterfaz();

        actualizarCantidadDesdeArchivo();
    }

    // ==========================================
    // CONFIGURAR VENTANA
    // ==========================================

    private void configurarVentana() {

        setTitle("FoxLift - Servidor");

        setSize(900, 650);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout(10, 10));
    }

    // ==========================================
    // CREAR INTERFAZ
    // ==========================================

    private void crearInterfaz() {

        // --------------------------------------
        // PANEL SUPERIOR
        // --------------------------------------

        JPanel panelSuperior = new JPanel(new GridLayout(2, 2, 10, 10));

        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        lblEstadoServidor = new JLabel("Servidor: INICIANDO...");

        lblEstadoSensor = new JLabel("Sensor: ESPERANDO");

        lblDistancia = new JLabel("Distancia: -- cm");

        lblTotal = new JLabel("Registros en estómago: 0");

        panelSuperior.add(lblEstadoServidor);

        panelSuperior.add(lblEstadoSensor);

        panelSuperior.add(lblDistancia);

        panelSuperior.add(lblTotal);

        add(panelSuperior, BorderLayout.NORTH);

        // --------------------------------------
        // TABLA
        // --------------------------------------

        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Fecha", "Hora", "Distancia", "Estado"}, 0) {

            @Override
            public boolean isCellEditable(int row, int column) {

                return false;
            }
        };

        tablaObjetos = new JTable(modeloTabla);

        JScrollPane scrollTabla = new JScrollPane(tablaObjetos);

        add(scrollTabla, BorderLayout.CENTER);

        // --------------------------------------
        // BOTONES
        // --------------------------------------

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnSimular = new JButton("Simular detección");

        btnLimpiar = new JButton("Limpiar tabla");

        panelBotones.add(btnSimular);

        panelBotones.add(btnLimpiar);

        // --------------------------------------
        // LOG
        // --------------------------------------

        areaLog = new JTextArea(8, 50);

        areaLog.setEditable(false);

        areaLog.setLineWrap(true);

        areaLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(areaLog);

        JPanel panelInferior = new JPanel(new BorderLayout());

        panelInferior.add(panelBotones, BorderLayout.NORTH);

        panelInferior.add(scrollLog, BorderLayout.CENTER);

        panelInferior.setBorder(BorderFactory.createTitledBorder("Registro del servidor"));

        add(panelInferior, BorderLayout.SOUTH);

        // --------------------------------------
        // BOTÓN SIMULAR
        // --------------------------------------

        btnSimular.addActionListener(e -> {

            Thread hiloSimulacion = new Thread(() -> {

                simulador.enviarMedicion();
            });

            hiloSimulacion.start();
        });

        // --------------------------------------
        // BOTÓN LIMPIAR
        // --------------------------------------

        btnLimpiar.addActionListener(e -> {

            modeloTabla.setRowCount(0);

            agregarLog("Tabla visual limpiada.");

            agregarLog("El archivo estomago.txt NO fue eliminado.");
        });
    }

    // ==========================================
    // INICIAR SERVIDOR
    // ==========================================

    public void iniciarServidor() {

        hiloServidor = new Thread(() -> {

            try {

                servidorSocket = new ServerSocket(PUERTO);

                SwingUtilities.invokeLater(() -> {

                    lblEstadoServidor.setText("Servidor: ACTIVO - Puerto " + PUERTO);
                });

                agregarLog("Servidor iniciado correctamente.");

                agregarLog("Esperando conexión del simulador...");

                while (!Thread.currentThread().isInterrupted()) {

                    try {

                        clienteSocket = servidorSocket.accept();

                        agregarLog("Simulador conectado: " + clienteSocket.getInetAddress().getHostAddress());

                        SwingUtilities.invokeLater(() -> {

                            lblEstadoSensor.setText("Sensor: CONECTADO");
                        });

                        atenderCliente(clienteSocket);

                    } catch (Exception e) {

                        if (servidorSocket != null && !servidorSocket.isClosed()) {

                            agregarLog("Error aceptando conexión: " + e.getMessage());
                        }
                    }
                }

                } catch (Exception e) {

                    agregarLog("ERROR iniciando servidor: " + e.getMessage());

                    SwingUtilities.invokeLater(() -> {

                        lblEstadoServidor.setText("Servidor: ERROR");
                    });
                }
        }, "Servidor-TCP");

        hiloServidor.start();
    }

    // ==========================================
    // ATENDER CLIENTE
    // ==========================================

    private void atenderCliente(Socket socket) {

        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String mensaje;

            while ((mensaje = entrada.readLine())!= null) {

                agregarLog("Mensaje recibido: " + mensaje);

                procesarMedicion(mensaje);
            }

        } catch (Exception e) {

            agregarLog("Error con el cliente: " + e.getMessage());

        } finally {

            try {

                socket.close();

            } catch (Exception ignored) {
            }

            SwingUtilities.invokeLater(() -> {

                lblEstadoSensor.setText("Sensor: ESPERANDO");
            });

            agregarLog("Simulador desconectado.");
        }
    }

    // ==========================================
    // PROCESAR MEDICIÓN
    // ==========================================

    private void procesarMedicion(
            String mensaje
    ) {

        if (mensaje == null) {

            return;
        }

        if (!mensaje.startsWith("DISTANCIA:")) {

            agregarLog("Mensaje desconocido recibido.");

            return;
        }

        try {

            String textoDistancia = mensaje.substring("DISTANCIA:".length());

            agregarLog("Texto recibido: [" + textoDistancia + "]");

            double distancia = Double.parseDouble(textoDistancia);

            SwingUtilities.invokeLater(() -> {

                lblDistancia.setText(String.format("Distancia: %.2f cm", distancia));
            });

            agregarLog(String.format("Medición recibida: %.2f cm", distancia));

            // ----------------------------------
            // COMPROBAR DETECCIÓN
            // ----------------------------------

            if (sensorHayObjeto(distancia)) {

                agregarLog("DISTANCIA DENTRO DEL RANGO DE DETECCIÓN.");

                registrarObjeto(distancia);

            } else {

                agregarLog("No se detectó ningún objeto.");
            }

        } catch (NumberFormatException e) {

            agregarLog("ERROR: No se pudo convertir la distancia.");

            agregarLog("Mensaje recibido: [" + mensaje + "]");
        }
    }

    // ==========================================
    // DETECCIÓN
    // ==========================================

    private boolean sensorHayObjeto(
            double distancia
    ) {

        return distancia <= LIMITE_DETECCION;
    }

    // ==========================================
    // REGISTRAR OBJETO
    // ==========================================

    private void registrarObjeto(double distancia) {

        try {

            agregarLog("OBJETO DETECTADO.");

            /*
             * RegistroObjeto se encarga de
             * proteger la escritura del archivo.
             */
            int id = registro.guardarObjeto(distancia);

            agregarLog("ID asignado: " + id);

            agregarLog("Captura guardada en estomago.txt.");

            agregarFila(id, distancia);

            actualizarCantidadDesdeArchivo();

        } catch (Exception e) {

            agregarLog("ERROR guardando objeto: " + e.getMessage());
        }
    }

    // ==========================================
    // ACTUALIZAR CANTIDAD DESDE ARCHIVO
    // ==========================================

    private void actualizarCantidadDesdeArchivo() {

        Thread hiloConsulta = new Thread(() -> {

            int cantidad = registro.contarRegistros();

            SwingUtilities.invokeLater(() -> {

                lblTotal.setText("Registros en estómago: " + cantidad);
            });
        }, "Consulta-Estomago");

        hiloConsulta.start();
    }

    // ==========================================
    // AGREGAR FILA
    // ==========================================

    private void agregarFila(int id, double distancia) {

        String fecha = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        SwingUtilities.invokeLater(() -> {

            modeloTabla.addRow(new Object[]{id, fecha, hora, String.format("%.2f cm",distancia),"OBJETO DETECTADO"});
        });
    }

    // ==========================================
    // MODO ESCAPE
    // ==========================================

    /**
     * Este método es llamado por el Auditor.
     *
     * La interfaz NO decide cuándo hay 3 registros.
     * El Auditor consulta estomago.txt y comunica
     * que puede activarse el Modo Escape.
     */
    public void modoEscape() {

        if (modoEscapeActivo) {

            return;
        }

        modoEscapeActivo = true;

        agregarLog("================================");

        agregarLog("AUDITOR: Se encontraron 3 registros válidos.");

        agregarLog("AUDITOR: PUEDE ACTIVARSE EL MODO ESCAPE.");

        agregarLog("================================");

        SwingUtilities.invokeLater(() -> {

            JOptionPane.showMessageDialog(this, "¡ATENCIÓN!\n\n" + "El Auditor detectó 3 registros " + "válidos en estomago.txt.\n\n" + "PUEDE ACTIVARSE EL MODO ESCAPE.", "MODO ESCAPE", JOptionPane.WARNING_MESSAGE);
        });
    }

    // ==========================================
    // LOG
    // ==========================================

    public void agregarLog(String mensaje) {

        SwingUtilities.invokeLater(() -> {

                    areaLog.append(mensaje + System.lineSeparator());

                    areaLog.setCaretPosition(areaLog.getDocument().getLength());
                }
        );
    }

    // ==========================================
    // CERRAR SERVIDOR
    // ==========================================

    @Override
    public void dispose() {

        try {

            if (clienteSocket != null && !clienteSocket.isClosed()) {

                clienteSocket.close();
            }

        } catch (Exception ignored) {
        }

        try {

            if (servidorSocket != null && !servidorSocket.isClosed()) {

                servidorSocket.close();
            }

        } catch (Exception ignored) {
        }

        if (hiloServidor != null) {

            hiloServidor.interrupt();
        }

        super.dispose();
    }
}
