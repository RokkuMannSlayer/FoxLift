package com.mycompany.foxlift;

public class CaracteristicasObjeto {

    private final double diametro;
    private final double peso;
    private final double redondez;
    private final double densidad;

    public CaracteristicasObjeto(
            double diametro,
            double peso,
            double redondez,
            double densidad
    ) {
        this.diametro = diametro;
        this.peso = peso;
        this.redondez = redondez;
        this.densidad = densidad;
    }

    public double getDiametro() {
        return diametro;
    }

    public double getPeso() {
        return peso;
    }

    public double getRedondez() {
        return redondez;
    }

    public double getDensidad() {
        return densidad;
    }

    /**
     * Convierte las características a valores
     * normalizados entre 0 y 1 para la red neuronal.
     */
    public double[] toInputVector() {

        return new double[]{
            diametro / 30.0,
            peso / 20.0,
            redondez,
            densidad
        };
    }

    @Override
    public String toString() {

        return String.format("Diámetro: %.2f mm | Peso: %.2f g | Redondez: %.3f | Densidad: %.3f", diametro, peso, redondez, densidad);
    }
}