package com.mycompany.foxlift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.List;

public class InterfazServidor extends JFrame {

    private static final int PUERTO = 5000;

    private final SimuladorSensor simulador;
    private final RegistroObjeto registro;
    private final ClasificadorCanica clasificador;

    private ServerSocket servidorSocket;

    private volatile boolean servidorActivo;

    private boolean modoEscapeActivo;

    // =====================================================
    // COMPONENTES DE INTERFAZ
    // =====================================================

    private JLabel lblEstadoServidor;
    private JLabel lblEstadoSensor;
    private JLabel lblDistancia;
    private JLabel lblTotal;

    private JTable tablaObjetos;

    private DefaultTableModel modeloTabla;

    private JTextArea areaLog;

    private JButton btnSimular;
    private JButton btnLimpiar;
    
    private JComboBox<String> comboPuertosBluetooth;

    private JButton btnBuscarBluetooth;
    private JButton btnConectarBluetooth;
    private JButton btnDesconectarBluetooth;

    private JButton btnEncenderArduino;
    private JButton btnApagarArduino;

    private JLabel lblEstadoBluetooth;

    private final LectorBluetooth lectorBluetooth = new LectorBluetooth();

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public InterfazServidor() {

        simulador = new SimuladorSensor();

        registro = new RegistroObjeto();

        /*
         * La red se carga o entrena al iniciar.
         */
        clasificador = new ClasificadorCanica();

        modoEscapeActivo = false;

        configurarVentana();

        crearInterfaz();

        actualizarCantidadDesdeArchivo();
    }
    
    private JPanel crearPanelBluetooth() {

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));

        panel.setBorder(BorderFactory.createTitledBorder("Control Bluetooth - Arduino"));

        comboPuertosBluetooth = new JComboBox<>();

        btnBuscarBluetooth = new JButton("Buscar puertos");
        btnConectarBluetooth = new JButton("Conectar");
        btnDesconectarBluetooth = new JButton("Desconectar");

        btnEncenderArduino = new JButton("Encender modo autónomo");
        btnApagarArduino = new JButton("Apagar modo autónomo");

        lblEstadoBluetooth = new JLabel("Bluetooth: desconectado");

        btnBuscarBluetooth.addActionListener(e -> buscarPuertosBluetooth());

        btnConectarBluetooth.addActionListener(e -> conectarBluetooth());

        btnDesconectarBluetooth.addActionListener(e -> desconectarBluetooth());

        btnEncenderArduino.addActionListener(e -> {

            if (lectorBluetooth.enviar("A")) {

                agregarLog("Comando A enviado al Arduino.");

            } else {

                agregarLog("Error enviando comando A.");
            }
        });

        btnApagarArduino.addActionListener(e -> {

            if (lectorBluetooth.enviar("M")) {

                agregarLog("Comando M enviado al Arduino.");

            } else {

                agregarLog("Error enviando comando M.");
            }
        });

        panel.add(new JLabel("Puerto:"));
        panel.add(comboPuertosBluetooth);
        panel.add(btnBuscarBluetooth);
        panel.add(btnConectarBluetooth);
        panel.add(btnDesconectarBluetooth);

        panel.add(btnEncenderArduino);
        panel.add(btnApagarArduino);
        panel.add(lblEstadoBluetooth);

        btnDesconectarBluetooth.setEnabled(false);
        btnEncenderArduino.setEnabled(false);
        btnApagarArduino.setEnabled(false);

        buscarPuertosBluetooth();

        return panel;
    }

    // =====================================================
    // CONFIGURAR VENTANA
    // =====================================================

    private void configurarVentana() {

        setTitle("FoxLift - Servidor Orquita");

        setSize(900, 650);

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout(10, 10));
    }

    // =====================================================
    // CREAR INTERFAZ
    // =====================================================
    
    private void buscarPuertosBluetooth() {

        comboPuertosBluetooth.removeAllItems();

        List<String> puertos = LectorBluetooth.detectarPuertos();

        for (String puerto : puertos) {
            comboPuertosBluetooth.addItem(puerto);
        }

        if (puertos.isEmpty()) {
            agregarLog("No se encontraron puertos COM.");
        } else {
            agregarLog("Puertos seriales encontrados: " + puertos.size());
        }
    }

    private void conectarBluetooth() {

        String seleccionado = (String) comboPuertosBluetooth.getSelectedItem();

        if (seleccionado == null) {
            agregarLog("Seleccioná un puerto Bluetooth.");
        return;
        }

        // El elemento tiene el formato: COM6 - Nombre del dispositivo.
        String nombrePuerto = seleccionado.split(" - ")[0].trim();

        boolean resultado = lectorBluetooth.iniciar(nombrePuerto, mensaje -> {

            agregarLog("BLUETOOTH RECIBIDO: [" + mensaje + "]");

            if (mensaje.equals("MODO_AUTONOMO:ACTIVADO")) {

                SwingUtilities.invokeLater(() -> {
                    lblEstadoBluetooth.setText(
                        "Arduino: modo autónomo ACTIVADO"
                    );
                });

            } else if (mensaje.equals("MODO_AUTONOMO:DESACTIVADO")) {

                SwingUtilities.invokeLater(() -> {
                    lblEstadoBluetooth.setText("Arduino: modo autónomo DESACTIVADO");
                });

            } else if (mensaje.startsWith("OBJETO:")) {

                procesarLecturaBluetooth(mensaje);
            }
        });

        if (resultado) {

            lblEstadoBluetooth.setText("Bluetooth: conectado (" + nombrePuerto + ")");

            btnBuscarBluetooth.setEnabled(false);
            btnConectarBluetooth.setEnabled(false);
            btnDesconectarBluetooth.setEnabled(true);

            btnEncenderArduino.setEnabled(true);
            btnApagarArduino.setEnabled(true);

            agregarLog("Conectado correctamente al Arduino por Bluetooth.");

        } else {

            agregarLog("No se pudo conectar al puerto " + nombrePuerto);
            JOptionPane.showMessageDialog(
                this,
                "No se pudo abrir el puerto Bluetooth.\nVerificá que esté emparejado y que ningún otro programa lo esté utilizando.",
                "Error Bluetooth",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    // =====================================================
// PROCESAR LECTURA DEL SENSOR BLUETOOTH
// =====================================================

    private void procesarLecturaBluetooth(String mensaje) {

        try {

            String valor = mensaje.substring(
                "OBJETO:".length()).trim();

            double distancia = Double.parseDouble(valor);

            if (!Double.isFinite(distancia) || distancia < 3.0 || distancia > 20.0) {

                agregarLog("Lectura descartada: fuera de rango.");
                return;
        }

        agregarLog("Procesando lectura del HC-SR04...");

            actualizarDistancia(distancia);

            actualizarEstadoSensor("Sensor Bluetooth: objeto detectado" );

            registrarObjeto(distancia, "SIN CLASIFICAR");

        } catch (Exception e) {

            agregarLog("Error procesando lectura Bluetooth: " + e.getMessage());
        }
    }

    private void desconectarBluetooth() {

        lectorBluetooth.detener();

        btnBuscarBluetooth.setEnabled(true);
        btnConectarBluetooth.setEnabled(true);
        btnDesconectarBluetooth.setEnabled(false);

        btnEncenderArduino.setEnabled(false);
        btnApagarArduino.setEnabled(false);

        lblEstadoBluetooth.setText("Bluetooth: desconectado");

        agregarLog("Conexión Bluetooth cerrada.");
    }

    private void enviarComandoArduino(String comando) {

        if (!lectorBluetooth.estaConectado()) {
            agregarLog("No hay conexión Bluetooth con Arduino.");
            return;
        }

        if (lectorBluetooth.enviar(comando)) {

            if ("A".equals(comando)) {
                agregarLog("Comando enviado: activar modo autónomo.");
            } else if ("M".equals(comando)) {
                agregarLog("Comando enviado: desactivar modo autónomo.");
            }

        } else {
            agregarLog("No se pudo enviar el comando al Arduino.");
        }
    }

    private void actualizarEstadoBluetooth(String texto) {

        SwingUtilities.invokeLater(() -> {
            lblEstadoBluetooth.setText(texto);
        });
    }

    private void crearInterfaz() {

        // -------------------------------------------------
        // PANEL SUPERIOR
        // -------------------------------------------------

        JPanel panelSuperior = new JPanel(new GridLayout(2, 2, 10, 5));

        lblEstadoServidor = new JLabel("Servidor: detenido");

        lblEstadoSensor = new JLabel("Sensor: esperando");

        lblDistancia = new JLabel("Distancia: -- cm");

        lblTotal = new JLabel("Registros: 0");

        panelSuperior.add(lblEstadoServidor);

        panelSuperior.add(lblEstadoSensor);

        panelSuperior.add(lblDistancia);

        panelSuperior.add(lblTotal);

        add(panelSuperior, BorderLayout.NORTH);

        // -------------------------------------------------
        // TABLA
        // -------------------------------------------------

        modeloTabla = new DefaultTableModel(new Object[]{"ID", "Fecha", "Hora", "Distancia", "Clasificación"}, 0) {

            @Override
            public boolean isCellEditable(int row,int column) {

                return false;
            }
        };

        tablaObjetos = new JTable(modeloTabla);

        JScrollPane scrollTabla = new JScrollPane(tablaObjetos);

        add(scrollTabla, BorderLayout.CENTER);

        // -------------------------------------------------
        // LOG
        // -------------------------------------------------

        areaLog = new JTextArea();

        areaLog.setEditable(false);

        areaLog.setRows(8);

        JScrollPane scrollLog = new JScrollPane(areaLog);

        // -------------------------------------------------
        // BOTONES
        // -------------------------------------------------

        btnSimular = new JButton("Simular detección");

        btnLimpiar = new JButton("Limpiar tabla");

        btnSimular.addActionListener(e -> {

            agregarLog("Solicitando medición al sensor...");

            simulador.enviarMedicion();
        });

        btnLimpiar.addActionListener(e -> {

            modeloTabla.setRowCount(0);

            agregarLog("Tabla limpiada.");
        });

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.CENTER));

        panelBotones.add(btnSimular);

        panelBotones.add(btnLimpiar);

        JPanel panelSur = new JPanel(new BorderLayout());

        panelSur.add(crearPanelBluetooth(), BorderLayout.NORTH);

        panelSur.add(scrollLog, BorderLayout.CENTER);

        panelSur.add(panelBotones, BorderLayout.SOUTH);

        add(panelSur, BorderLayout.SOUTH);
    }

    // =====================================================
    // INICIAR SERVIDOR
    // =====================================================

    public void iniciarServidor() {

        if (servidorActivo) {
            return;
        }

        servidorActivo = true;

        Thread hiloServidor = new Thread(() -> ejecutarServidor(), "Servidor-TCP");

        hiloServidor.start();
    }

    private void ejecutarServidor() {

        try {

            servidorSocket = new ServerSocket(PUERTO);

            agregarLog("Servidor TCP iniciado.");

            agregarLog("Puerto: " + PUERTO);

            actualizarEstadoServidor("Servidor: activo");

            while (servidorActivo) {

                Socket socket = servidorSocket.accept();

                agregarLog("Sensor conectado: " + socket.getInetAddress());

                Thread hiloCliente = new Thread(() -> atenderCliente(socket), "Cliente-Sensor");

                hiloCliente.start();
            }

        } catch (Exception e) {

            if (servidorActivo) {

                agregarLog("Error del servidor: " + e.getMessage());
            }
        }
    }

    // =====================================================
    // ATENDER CLIENTE
    // =====================================================

    private void atenderCliente(Socket socket) {

        try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String mensaje;

            while ((mensaje = entrada.readLine())!= null) {

                procesarMedicion(mensaje);
            }

        } catch (Exception e) {

            agregarLog("Error atendiendo sensor: "+ e.getMessage());

        } finally {

            try {
                socket.close();
            } catch (Exception ignored) {
            }

            agregarLog("Sensor desconectado.");

            actualizarEstadoSensor("Sensor: desconectado");
        }
    }

    // =====================================================
    // PROCESAR MEDICIÓN
    // =====================================================

    private void procesarMedicion(
            String mensaje) {

        if (mensaje == null) {
            return;
        }

        if (!mensaje.startsWith("DISTANCIA:")) {

            agregarLog("Mensaje desconocido recibido.");

            return;
        }

        try {

            /*
             * Formato:
             *
             * DISTANCIA:13.72;
             * DIAMETRO:18.42;
             * PESO:5.37;
             * REDONDEZ:0.981;
             * DENSIDAD:0.947
             */

            String[] partes = mensaje.split(";");

            if (partes.length < 5) {

                throw new Exception("Mensaje incompleto.");
            }

            double distancia = Double.parseDouble(partes[0].split(":")[1]);

            double diametro = Double.parseDouble(partes[1].split(":")[1]);

            double peso = Double.parseDouble(partes[2].split(":")[1]);

            double redondez = Double.parseDouble(partes[3].split(":")[1]);

            double densidad = Double.parseDouble(partes[4].split(":")[1]);

            actualizarDistancia(distancia);

            actualizarEstadoSensor("Sensor: medición recibida");

            agregarLog(String.format(Locale.US, "Medición recibida: %.2f cm", distancia));

            // -------------------------------------------------
            // DETECCIÓN
            // -------------------------------------------------

            if (sensorHayObjeto(distancia)) {

                agregarLog("DISTANCIA DENTRO DEL RANGO " + "DE DETECCIÓN.");

                // -------------------------------------------------
                // CARACTERÍSTICAS
                // -------------------------------------------------

                CaracteristicasObjeto objeto = new CaracteristicasObjeto(diametro, peso, redondez, densidad);

                agregarLog("Características del objeto:");

                agregarLog(objeto.toString());

                // -------------------------------------------------
                // RED NEURONAL
                // -------------------------------------------------

                String clasificacion = clasificador.clasificar(objeto);

                double[] salida = clasificador.obtenerSalida(objeto);

                agregarLog("RED NEURONAL:");

                agregarLog(String.format(Locale.US, "CANICA: %.4f", salida[0]));

                agregarLog(String.format(Locale.US, "NO CANICA: %.4f", salida[1]));

                agregarLog("CLASIFICACIÓN: " + clasificacion);

                // -------------------------------------------------
                // GUARDAR CAPTURA
                // -------------------------------------------------

                registrarObjeto(distancia, clasificacion);

            } else {

                agregarLog("No se detectó ningún objeto.");
            }

        } catch (Exception e) {

            agregarLog("ERROR procesando medición: " + e.getMessage());
        }
    }

    // =====================================================
    // DETECTAR OBJETO
    // =====================================================

    private boolean sensorHayObjeto(double distancia) {

        return distancia <= simulador.getDistanciaDeteccion();
    }

    // =====================================================
    // REGISTRAR OBJETO
    // =====================================================

    private void registrarObjeto(double distancia, String clasificacion) {

        try {

            agregarLog("Registrando lectura del sensor...");

            int id = registro.guardarObjeto(distancia, clasificacion);

            agregarLog("Registro guardado correctamente.");
            agregarLog("ID asignado: " + id);

            agregarLog(String.format(Locale.US, "Distancia registrada: %.2f cm", distancia));

            agregarLog("Clasificación: " + clasificacion);
            agregarLog("Archivo: estomago.txt");

            agregarFila(id, distancia, clasificacion);

            actualizarCantidadDesdeArchivo();

        } catch (Exception e) {

            agregarLog("ERROR guardando lectura: " + e.getMessage());
        }
    }

    // =====================================================
    // AGREGAR FILA
    // =====================================================

    private void agregarFila(int id, double distancia, String clasificacion) {

        String fecha = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String hora = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        SwingUtilities.invokeLater(() -> {

            modeloTabla.addRow(new Object[]{
                id,
                fecha,
                hora,
                String.format(Locale.US, "%.2f cm", distancia),
                clasificacion
            });
        });
    }

    // =====================================================
    // ACTUALIZAR CANTIDAD
    // =====================================================

    private void actualizarCantidadDesdeArchivo() {

        int cantidad = registro.contarRegistros();

        SwingUtilities.invokeLater(() -> {

            lblTotal.setText("Registros: " + cantidad);
        });
    }

    // =====================================================
    // MODO ESCAPE
    // =====================================================

    public void modoEscape() {

        SwingUtilities.invokeLater(() -> {

            if (modoEscapeActivo) {
                return;
            }

            modoEscapeActivo = true;

            agregarLog("================================");

            agregarLog("MODO ESCAPE DISPONIBLE");

            agregarLog("El Auditor detectó " + "3 registros válidos.");

            agregarLog("================================");

            JOptionPane.showMessageDialog(this, "¡ATENCIÓN!\n\n" + "El Auditor detectó " + "3 registros válidos " + "en estomago.txt.\n\n" + "PUEDE ACTIVARSE EL " + "MODO ESCAPE.", "Modo Escape", JOptionPane.WARNING_MESSAGE);
        });
    }

    // =====================================================
    // LOG
    // =====================================================

    public void agregarLog(String mensaje) {

        SwingUtilities.invokeLater(() -> {

            areaLog.append(mensaje + "\n");

            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    // =====================================================
    // ACTUALIZAR ESTADO SERVIDOR
    // =====================================================

    private void actualizarEstadoServidor(String texto) {

        SwingUtilities.invokeLater(() -> {

            lblEstadoServidor.setText(texto);
        });
    }

    // =====================================================
    // ACTUALIZAR ESTADO SENSOR
    // =====================================================

    private void actualizarEstadoSensor(String texto) {

        SwingUtilities.invokeLater(() -> {

            lblEstadoSensor.setText(texto);
        });
    }

    // =====================================================
    // ACTUALIZAR DISTANCIA
    // =====================================================

    private void actualizarDistancia(double distancia) {

        SwingUtilities.invokeLater(() -> {

            lblDistancia.setText(String.format(Locale.US, "Distancia: %.2f cm", distancia));
        });
    }

    // =====================================================
    // CERRAR SERVIDOR
    // =====================================================

    public void detenerServidor() {

        servidorActivo = false;
        lectorBluetooth.detener();

        try {

            if (servidorSocket != null) {
                servidorSocket.close();
            }

        } catch (Exception e) {

            agregarLog("Error cerrando servidor: " + e.getMessage());
        }
    }
}