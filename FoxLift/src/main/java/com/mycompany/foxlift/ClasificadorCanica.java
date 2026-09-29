package com.mycompany.foxlift;

import org.neuroph.core.NeuralNetwork;
import org.neuroph.core.data.DataSet;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.io.File;

public class ClasificadorCanica {

    private static final String ARCHIVO_RED = "red_canicas.nnet";

    private NeuralNetwork<?> redNeuronal;

    public ClasificadorCanica() {
        
        inicializarRed();
    }

    // =====================================================
    // INICIALIZAR RED
    // =====================================================

    private void inicializarRed() {

        File archivo = new File(ARCHIVO_RED);

        if (archivo.exists()) {

            try {

                redNeuronal = NeuralNetwork.load(ARCHIVO_RED);

                System.out.println("Red neuronal cargada correctamente.");

                return;

            } catch (Exception e) {

                System.out.println("No se pudo cargar la red neuronal.");

                System.out.println("Se entrenará una nueva.");
            }
        }

        entrenarRed();
    }

    // =====================================================
    // ENTRENAR RED
    // =====================================================

    private void entrenarRed() {

        System.out.println("Entrenando red neuronal...");

        /*
         * 4 entradas:
         *
         * 1 - Diámetro
         * 2 - Peso
         * 3 - Redondez
         * 4 - Densidad
         *
         * 2 salidas:
         *
         * [1,0] = CANICA
         * [0,1] = NO CANICA
         */

        DataSet conjuntoEntrenamiento = new DataSet(4, 2);

        // =================================================
        // CANICAS
        // =================================================

        agregarCanica(conjuntoEntrenamiento, 16.0, 4.5, 0.97, 0.95);

        agregarCanica(conjuntoEntrenamiento, 18.0, 5.2, 0.99, 0.96);

        agregarCanica(conjuntoEntrenamiento, 20.0, 6.1, 0.98, 0.94);

        agregarCanica(conjuntoEntrenamiento, 22.0, 7.0, 0.96, 0.97);

        agregarCanica(conjuntoEntrenamiento, 15.0, 3.8, 0.95, 0.93);

        agregarCanica(conjuntoEntrenamiento, 19.0, 5.8, 0.99, 0.98);

        agregarCanica(conjuntoEntrenamiento, 23.0, 7.5, 0.97, 0.95);

        agregarCanica(conjuntoEntrenamiento, 17.0, 4.2, 0.96, 0.94);

        agregarCanica(conjuntoEntrenamiento, 21.0, 6.8, 0.98, 0.96);

        // =================================================
        // NO CANICAS
        // =================================================

        agregarNoCanica(conjuntoEntrenamiento, 5.0, 1.0, 0.60, 0.30);

        agregarNoCanica(conjuntoEntrenamiento, 30.0, 15.0, 0.70, 0.50);

        agregarNoCanica(conjuntoEntrenamiento,10.0, 12.0, 0.55, 0.40);

        agregarNoCanica(conjuntoEntrenamiento, 28.0, 3.0, 0.65, 0.30);

        agregarNoCanica(conjuntoEntrenamiento, 8.0, 8.0, 0.50, 0.70);

        agregarNoCanica(conjuntoEntrenamiento, 30.0, 18.0, 0.40, 0.80);

        agregarNoCanica(conjuntoEntrenamiento, 12.0, 2.0, 0.70, 0.20);

        agregarNoCanica(conjuntoEntrenamiento, 27.0, 10.0, 0.75, 0.60);

        agregarNoCanica(conjuntoEntrenamiento, 7.0, 4.0, 0.45, 0.25);

        // =================================================
        // CREAR RED
        // =================================================

        /*
         * Arquitectura:
         *
         * 4 entradas
         * 6 neuronas ocultas
         * 2 salidas
         */

        redNeuronal = new MultiLayerPerceptron(TransferFunctionType.SIGMOID, 4, 6, 2);

        // =================================================
        // ENTRENAMIENTO
        // =================================================

        redNeuronal.learn(conjuntoEntrenamiento);

        System.out.println("Entrenamiento finalizado.");

        // =================================================
        // GUARDAR RED
        // =================================================

        try {

            redNeuronal.save(ARCHIVO_RED);

            System.out.println("Red neuronal guardada en: " + ARCHIVO_RED);

        } catch (Exception e) {

            System.out.println("Error guardando la red: " + e.getMessage());
        }
    }

    // =====================================================
    // AGREGAR CANICA
    // =====================================================

    private void agregarCanica(DataSet conjunto, double diametro, double peso, double redondez, double densidad) {

        CaracteristicasObjeto objeto = new CaracteristicasObjeto(diametro, peso, redondez, densidad);

        /*
         * [1, 0] significa CANICA.
         *
         * Usamos add() en lugar de crear
         * manualmente un DataSetRow.
         */

        conjunto.add(objeto.toInputVector(), new double[]{1.0, 0.0});
    }

    // =====================================================
    // AGREGAR NO CANICA
    // =====================================================

    private void agregarNoCanica(DataSet conjunto, double diametro, double peso, double redondez, double densidad) {

        CaracteristicasObjeto objeto = new CaracteristicasObjeto(diametro, peso, redondez, densidad);

        /*
         * [0, 1] significa NO CANICA.
         */

        conjunto.add(objeto.toInputVector(), new double[]{0.0, 1.0});
    }

    // =====================================================
    // CLASIFICAR
    // =====================================================

    public String clasificar(CaracteristicasObjeto objeto) {

        double[] salida = obtenerSalida(objeto);

        if (salida[0] > salida[1]) {

            return "CANICA";

        } else {

            return "NO CANICA";
        }
    }

    // =====================================================
    // OBTENER SALIDA
    // =====================================================

    public double[] obtenerSalida(CaracteristicasObjeto objeto) {

        redNeuronal.setInput(objeto.toInputVector());

        redNeuronal.calculate();

        return redNeuronal.getOutput();
    }
}