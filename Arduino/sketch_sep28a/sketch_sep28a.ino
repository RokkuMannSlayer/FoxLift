```cpp
#include <SoftwareSerial.h>

// =====================================================
// BLUETOOTH HC-05
// =====================================================

// Arduino RX = pin 2
// Arduino TX = pin 3

SoftwareSerial bluetooth(2, 3);


// =====================================================
// SENSOR HC-SR04
// =====================================================

const int TRIG_PIN = 9;
const int ECHO_PIN = 10;


// =====================================================
// CONFIGURACIÓN
// =====================================================

const float DISTANCIA_DETECCION = 20.0;
const float DISTANCIA_MINIMA = 3.0;

const unsigned long INTERVALO_MEDICION = 200;

const unsigned long TIEMPO_REARME = 1500;


// =====================================================
// VARIABLES
// =====================================================

bool canicaDetectada = false;

unsigned long ultimaDeteccion = 0;


// =====================================================
// SETUP
// =====================================================

void setup() {

  // Monitor serial de Arduino
  Serial.begin(9600);

  // Bluetooth
  bluetooth.begin(9600);

  // HC-SR04
  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  digitalWrite(TRIG_PIN, LOW);

  Serial.println("================================");
  Serial.println("FOX LIFT");
  Serial.println("Sistema iniciado");
  Serial.println("================================");

  bluetooth.println("SERVIDOR_ARDUINO_LISTO");
}


// =====================================================
// LOOP
// =====================================================

void loop() {

  float distancia = medirDistancia();

  // ---------------------------------------------
  // Mostrar distancia
  // ---------------------------------------------

  Serial.print("Distancia: ");
  Serial.print(distancia, 2);
  Serial.println(" cm");


  // ---------------------------------------------
  // Detectar objeto
  // ---------------------------------------------

  if (
    distancia >= DISTANCIA_MINIMA &&
    distancia <= DISTANCIA_DETECCION
  ) {

    detectarCanica(distancia);

  } else {

    /*
     * El objeto se retiró.
     * El sensor queda preparado para
     * detectar otra canica.
     */

    canicaDetectada = false;

    Serial.println("Sin objeto");

  }


  delay(INTERVALO_MEDICION);
}


// =====================================================
// MEDIR DISTANCIA
// =====================================================

float medirDistancia() {

  digitalWrite(TRIG_PIN, LOW);

  delayMicroseconds(2);

  digitalWrite(TRIG_PIN, HIGH);

  delayMicroseconds(10);

  digitalWrite(TRIG_PIN, LOW);


  long duracion =
    pulseIn(
      ECHO_PIN,
      HIGH,
      30000
    );


  // No hubo eco
  if (duracion == 0) {

    return 0;
  }


  float distancia =
    duracion * 0.0343 / 2.0;


  return distancia;
}


// =====================================================
// DETECTAR CANICA
// =====================================================

void detectarCanica(float distancia) {

  unsigned long ahora =
    millis();


  /*
   * Evitar registrar varias veces
   * la misma canica.
   */

  if (canicaDetectada) {

    return;
  }


  /*
   * Tiempo mínimo entre registros.
   */

  if (
    ahora - ultimaDeteccion
    < TIEMPO_REARME
  ) {

    return;
  }


  // ===================================================
  // CANICA DETECTADA
  // ===================================================

  canicaDetectada = true;

  ultimaDeteccion = ahora;


  Serial.println(
    "================================"
  );

  Serial.println(
    "CANICA DETECTADA"
  );

  Serial.print(
    "Distancia: "
  );

  Serial.print(
    distancia,
    2
  );

  Serial.println(
    " cm"
  );


  // ===================================================
  // ENVIAR AL SERVIDOR JAVA
  // ===================================================

  /*
   * Protocolo:
   *
   * CANICA:13.72
   *
   * Java recibirá exactamente ese mensaje.
   */

  bluetooth.print(
    "CANICA:"
  );

  bluetooth.println(
    distancia,
    2
  );


  Serial.println(
    "Registro enviado al servidor Java."
  );

  Serial.println(
    "================================"
  );
}
```
