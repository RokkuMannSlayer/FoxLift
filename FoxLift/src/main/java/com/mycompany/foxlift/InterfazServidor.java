package com.mycompany.foxlift;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.ServerSocket;
import java.net.Socket;

import java.text.DecimalFormat;

/**
 * Interfaz gráfica y servidor TCP de FoxLift.
 *
 * El servidor recibe las mediciones enviadas por
 * el simulador del sensor mediante Socket TCP.
 */
public class InterfazServidor extends JFrame {

    // Puerto TCP utilizado por FoxLift
    private static final int PUERTO = 5000;

    private ServerSocket servidorSocket;

    private Socket clienteSocket;

    private Thread hiloServidor;

    private SimuladorSensor simulador;

    private RegistroObjeto registro;

    private JTable tablaObjetos;

    private DefaultTableModel modeloTabla;

    private JTextArea areaLog;

    private JLabel lblEstadoServidor;

    private JLabel lblEstadoSensor;

    private JLabel lblDistancia;

    private JLabel lblTotal;

    private int totalObjetos;

    private final DecimalFormat formatoDistancia = new DecimalFormat("0.00");

    public InterfazServidor() {

        simulador = new SimuladorSensor();

        registro = new RegistroObjeto();

        totalObjetos = 0;

        configurarVentana();

        crearInterfaz();
    }

    /**
     * Configura la ventana.
     */
    private void configurarVentana() {

        setTitle("FoxLift - Servidor TCP");

        setSize(900, 650);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setResizable(false);
    }

    /**
     * Construye la interfaz gráfica.
     */
    private void crearInterfaz() {

        setLayout(new BorderLayout(10, 10));

        /*
         * ==========================================
         * PANEL SUPERIOR
         * ==========================================
         */

        JPanel panelSuperior = new JPanel(new BorderLayout());

        panelSuperior.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel titulo = new JLabel("FoxLift - Servidor de Detección", SwingConstants.CENTER);

        titulo.setFont(new Font("Arial", Font.BOLD, 24));

        panelSuperior.add(titulo, BorderLayout.CENTER);

        /*
         * Estado del servidor
         */

        lblEstadoServidor = new JLabel("SERVIDOR: INICIANDO...", SwingConstants.CENTER);

        lblEstadoServidor.setFont(new Font("Arial", Font.BOLD, 14));

        panelSuperior.add(lblEstadoServidor, BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);

        /*
         * ==========================================
         * PANEL CENTRAL
         * ==========================================
         */

        JPanel panelCentral = new JPanel(new BorderLayout(10, 10));

        panelCentral.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        /*
         * ==========================================
         * INFORMACIÓN
         * ==========================================
         */

        JPanel panelInformacion = new JPanel(new GridLayout(1, 3, 10, 10));

        lblEstadoSensor = new JLabel("SENSOR: ESPERANDO", SwingConstants.CENTER);

        lblEstadoSensor.setFont(new Font("Arial", Font.BOLD, 15));

        lblDistancia = new JLabel("Distancia: -- cm",  SwingConstants.CENTER);

        lblDistancia.setFont(new Font("Arial", Font.BOLD, 15));

        lblTotal = new JLabel("Objetos detectados: 0", SwingConstants.CENTER);

        lblTotal.setFont(new Font("Arial", Font.BOLD, 15));

        panelInformacion.add(lblEstadoSensor);

        panelInformacion.add(lblDistancia);

        panelInformacion.add(lblTotal);

        panelCentral.add(panelInformacion, BorderLayout.NORTH);

        /*
         * ==========================================
         * TABLA
         * ==========================================
         */

        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Fecha", "Hora", "Distancia", "Estado"}, 0) {

                    @Override
                    public boolean isCellEditable(int fila, int columna) {

                        return false;
                    }
                };

        tablaObjetos = new JTable(modeloTabla);

        tablaObjetos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tablaObjetos.setRowHeight(25);

        tablaObjetos.setFont(new Font("Arial", Font.PLAIN, 13));

        tablaObjetos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));

        JScrollPane scrollTabla = new JScrollPane(tablaObjetos);

        panelCentral.add(scrollTabla, BorderLayout.CENTER);

        add(panelCentral, BorderLayout.CENTER);

        /*
         * ==========================================
         * PANEL INFERIOR
         * ==========================================
         */

        JPanel panelInferior = new JPanel(new BorderLayout(10, 10));

        panelInferior.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        /*
         * BOTONES
         */

        JPanel panelBotones = new JPanel();

        JButton btnSimular = new JButton("Simular detección");

        btnSimular.setFont(new Font("Arial", Font.BOLD, 14));

        btnSimular.setPreferredSize(new Dimension(200, 40));

        JButton btnLimpiar = new JButton("Limpiar tabla");

        btnLimpiar.setFont(new Font("Arial", Font.BOLD, 14));

        btnLimpiar.setPreferredSize(new Dimension(160, 40));

        panelBotones.add(btnSimular);

        panelBotones.add(btnLimpiar);

        panelInferior.add(panelBotones, BorderLayout.NORTH);

        /*
         * LOG
         */

        areaLog = new JTextArea(6, 20);

        areaLog.setEditable(false);

        areaLog.setFont(new Font("Monospaced",Font.PLAIN, 12));

        areaLog.setText("FoxLift iniciado.\n" + "Esperando conexión TCP...\n");

        JScrollPane scrollLog =new JScrollPane(areaLog);

        panelInferior.add(scrollLog, BorderLayout.CENTER);

        add(panelInferior, BorderLayout.SOUTH);

        /*
         * ==========================================
         * EVENTOS
         * ==========================================
         */

        btnSimular.addActionListener(e -> {

            // Crear un hilo para no congelar la interfaz
            Thread hilo = new Thread(() -> {

                        simulador.enviarMedicion();

                    });

            hilo.start();
        });

        btnLimpiar.addActionListener(e -> {

            modeloTabla.setRowCount(0);

            totalObjetos = 0;

            lblTotal.setText("Objetos detectados: 0");

            agregarLog("Tabla limpiada.");
        });
    }

    /**
     * Inicia el servidor TCP en el puerto 5000.
     */
    public void iniciarServidor() {

        hiloServidor = new Thread(() -> {

                    try {

                        servidorSocket = new ServerSocket(PUERTO);

                        SwingUtilities.invokeLater(() -> {

                            lblEstadoServidor.setText("SERVIDOR: ESCUCHANDO PUERTO " + PUERTO);

                        });

                        agregarLog("Servidor TCP iniciado.");

                        agregarLog("Puerto: " + PUERTO);

                        /*
                         * El servidor permanece esperando
                         * conexiones del simulador.
                         */

                        while (true) {

                            clienteSocket = servidorSocket.accept();

                            agregarLog("Simulador conectado: " + clienteSocket.getInetAddress().getHostAddress());

                            atenderCliente(clienteSocket);
                        }

                    } catch (IOException e) {

                        agregarLog(
                                "Error del servidor: "
                                + e.getMessage()
                        );
                    }

                });

        hiloServidor.start();
    }

    /**
     * Atiende una conexión proveniente del simulador.
     */
    private void atenderCliente(Socket socket) {

        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String mensaje;

            while ((mensaje = entrada.readLine()) != null) {

                procesarMedicion(mensaje);
            }

        } catch (IOException e) {

            agregarLog("Conexión finalizada.");
        }
    }

    /**
     * Procesa una medición recibida.
     *
     * El simulador envía:
     *
     * DISTANCIA:12.50
     */
    private void procesarMedicion(String mensaje) {

        try {

            if (!mensaje.startsWith("DISTANCIA:")) {

                agregarLog("Mensaje desconocido: " + mensaje);

                return;
            }

            String textoDistancia = mensaje.substring("DISTANCIA:".length());

            double distancia = Double.parseDouble(textoDistancia);

            SwingUtilities.invokeLater(() -> {

                lblDistancia.setText("Distancia: " + formatoDistancia.format(distancia) + " cm");

            });

            agregarLog("Medición recibida: " + formatoDistancia.format(distancia) + " cm" );

            /*
             * ==================================
             * DETECCIÓN
             * ==================================
             */

            if (sensorHayObjeto(distancia)) {

                agregarLog("DISTANCIA DENTRO DEL RANGO DE DETECCIÓN.");

                RegistroObjeto.Registro objeto = registro.guardarObjeto(distancia);

                totalObjetos++;

                SwingUtilities.invokeLater(() -> {

                    lblEstadoSensor.setText("SENSOR: OBJETO DETECTADO");

                    lblTotal.setText("Objetos detectados: " + totalObjetos);

                    modeloTabla.addRow(new Object[]{objeto.getId(), objeto.getFecha(), objeto.getHora(), formatoDistancia.format(objeto.getDistancia()) + " cm", objeto.getEstado()});
                });

                agregarLog("OBJETO DETECTADO.");

                agregarLog("ID asignado: " + objeto.getId());

                agregarLog("Guardado en estomago.txt.");

            } else {

                agregarLog("DISTANCIA FUERA DEL RANGO DE DETECCIÓN.");

                SwingUtilities.invokeLater(() -> {

                    lblEstadoSensor.setText("SENSOR: SIN OBJETO");
                });

                agregarLog("No se detectó ningún objeto.");
            }

        } catch (
                NumberFormatException e
        ) {

            agregarLog("Distancia inválida: " + mensaje);
        }
    }

    /**
     * Determina si hay un objeto.
     */
    private boolean sensorHayObjeto(double distancia) {

        final double LIMITE_DETECCION = 20.0;
        
        return distancia <= LIMITE_DETECCION;
    }

    /**
     * Agrega mensajes al área de log.
     */
    private void agregarLog(String mensaje) {

        SwingUtilities.invokeLater(() -> {

            areaLog.append(mensaje + "\n");

            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    /**
     * Cierra correctamente el servidor.
     */
    @Override
    public void dispose() {

        try {

            if (clienteSocket != null && !clienteSocket.isClosed()) {

                clienteSocket.close();
            }

            if (servidorSocket != null && !servidorSocket.isClosed()) {

                servidorSocket.close();
            }

        } catch (IOException e) {

            System.err.println("Error cerrando sockets: " + e.getMessage());
        }

        super.dispose();
    }
}